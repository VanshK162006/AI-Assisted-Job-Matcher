package com.project.job_finder_ai;

import com.project.job_finder_ai.dto.CompanyRequest;
import com.project.job_finder_ai.entity.Company;
import com.project.job_finder_ai.enums.AtsType;
import com.project.job_finder_ai.exception.DuplicateResourceException;
import com.project.job_finder_ai.repository.CompanyRepository;
import com.project.job_finder_ai.service.directRepositoryConnection.CompanyServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @InjectMocks
    private CompanyServices companyServices;

    private CompanyRequest mockCompanyRequest(){
        return new CompanyRequest("Stripe", AtsType.GREENHOUSE, "stripe", "https://stripe.com/jobs");
    }

    @Test
    void addANewCompany(){
        when(companyRepository.existsByAtsTypeAndBoardSlug(AtsType.GREENHOUSE,"stripe")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(
                (inv)->inv.getArgument(0)
        );

        companyServices.addCompany(mockCompanyRequest());

        ArgumentCaptor<Company> saved=ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(saved.capture());

        assertEquals("Stripe",saved.getValue().getName());
        assertEquals("stripe",saved.getValue().getBoardSlug());
    }

    @Test
    void refusesDuplicateSavingOfCompany(){
        when(companyRepository.existsByAtsTypeAndBoardSlug(AtsType.GREENHOUSE,"stripe")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,()->companyServices.addCompany(mockCompanyRequest()));
        verify(companyRepository,never()).save(any());
    }
}
