package com.project.job_finder_ai.service.ai;

import com.openai.services.blocking.finetuning.JobService;
import com.project.job_finder_ai.config.AgentProperties;
import com.project.job_finder_ai.dto.AgentReply;
import com.project.job_finder_ai.exception.AiUnavailableException;
import com.project.job_finder_ai.exception.BadRequestException;
import com.project.job_finder_ai.repository.JobMatchRepository;
import com.project.job_finder_ai.repository.JobRepository;
import com.project.job_finder_ai.service.JobMatchService;
import com.project.job_finder_ai.service.JobServices;
import com.project.job_finder_ai.service.directRepositoryConnection.CandidateProfileService;
import com.project.job_finder_ai.service.directRepositoryConnection.CompanyServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Service
@Slf4j
public class AgentService {


    private final ChatClient chatClient;
    private final AgentProperties props;
    private final AgentPromptBuilder promptBuilder;
    private final JobMatchService jobMatchService;
    private final JobServices jobService;
    private final CandidateProfileService profileService;
    private final CompanyServices companyService;
    private final JobRepository jobRepository;
    private final JobMatchRepository matchRepository;

    public AgentService(ChatClient.Builder builder,
                        AgentProperties props,
                        AgentPromptBuilder promptBuilder, JobMatchService jobMatchService,
                        JobServices jobService, CandidateProfileService profileService,
                        CompanyServices companyService, JobRepository jobRepository,
                        JobMatchRepository matchRepository, ChatMemory chatMemory) {

        this.chatClient = builder
                .defaultSystem(AgentPromptBuilder.SYSTEM)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        this.props = props;
        this.promptBuilder = promptBuilder;
        this.jobMatchService = jobMatchService;
        this.jobService = jobService;
        this.profileService = profileService;
        this.companyService = companyService;
        this.jobRepository = jobRepository;
        this.matchRepository = matchRepository;
    }

    public AgentReply chat(String conversationId, String message) {
        if (!props.isEnabled()) {
            throw new BadRequestException("The assistant is switched off (agent.enabled=false)");
        }
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Message must not be empty");
        }
        if (text.length() > props.getMaxMessageChars()) {
            throw new BadRequestException(
                    "Message is too long (max " + props.getMaxMessageChars() + " characters)");
        }
        String id = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString() : conversationId.trim();
        if (!CONVERSATION_ID.matches(id)) {
            throw new BadRequestException(
                    "conversationId may only contain letters, digits, - and _ (max 64 characters)");
        }

        AgentTrace trace = new AgentTrace(props.getMaxToolCalls());
        AgentTools tools = new AgentTools(jobMatchService, jobService, profileService,
                companyService, jobRepository, matchRepository, trace, props.getMaxResultsPerTool());
        String prompt = promptBuilder.build(text);

        String reply;
        try {
            reply = chatClient.prompt()
                    .user(prompt)
                    .tools(tools)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Assistant call failed: {}", e.getMessage());
            throw new AiUnavailableException("The assistant could not answer right now", e);
        }
        if (reply == null || reply.isBlank()) {
            reply = "I could not produce an answer. Please try rephrasing.";
        }

        log.info("Assistant answered using {} tool call(s)", trace.calls().size());
        return new AgentReply(id, reply, trace.calls());
    }
}