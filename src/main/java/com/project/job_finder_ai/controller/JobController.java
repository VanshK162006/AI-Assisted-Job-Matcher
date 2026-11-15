/*
# HOW TO TAKE DECISIONS AND UNDERSTAND LOGIC (WITHOUT AI)

## PART 1: Before writing any code (10 minutes)

STEP 1. Write the problem in ONE sentence.
  Example: "From 3,000 saved jobs, keep only Java jobs in India
  that don't need many years of experience."
  If I can't write one sentence, I don't understand the problem yet.

STEP 2. Write INPUT and OUTPUT.
  Input  : a job (title, location, description)
  Output : pass / fail + reason

STEP 3. Make an EXAMPLE TABLE (5 to 10 rows) BEFORE code.
  | Input                      | Expected |
  | Java Developer, Pune       | keep     |
  | Senior Java Developer      | reject   |
  | JavaScript Engineer, Pune  | reject   |
  | Java Developer, London     | reject   |
  This table becomes my test cases later.

STEP 4. Underline NOUNS and VERBS in my sentence.
  Nouns (things)   -> data / entity / class   (Job, Company)
  Verbs (actions)  -> methods                 (check, save, fetch, count)

## PART 2: Which class does this belong to? (ask in this order)

  1. Does it talk to something OUTSIDE (internet, AI, Telegram)?
        -> Fetcher / Sender class
  2. Does it make a DECISION (if/else rules)?
        -> Service (pure logic, no database inside)
  3. Does it run STEPS in order or LOOP and save?
        -> Coordinator service
  4. Does it only SAVE or LOAD data?
        -> Repository
  5. Does it only RECEIVE a web request and return a response?
        -> Controller
  6. Is it a fixed list of allowed values?
        -> Enum
  7. Is it just a bag of data to carry around?
        -> record / DTO
  8. Is it a row in the database?
        -> Entity

  RULE: if I can't describe a class in ONE sentence, it does too much. Split it.

## PART 3: Build in small safe steps

STEP 5. Write the DUMBEST version that works.
  Hard-code everything. Make the example table pass.

STEP 6. Run the table. Every FAILURE tells me what to improve.
  | Problem I see                     | Fix                          |
  | Wrong match (java vs JavaScript)  | Match whole words (regex \b) |
  | Values hard-coded                 | Move to application.yml      |
  | Crash on empty value              | Check null first             |
  | Can't tell WHY it failed          | Return a reason with answer  |
  | One method is too long            | Split into small methods     |
  | Result must be remembered         | Add a column + repository    |

STEP 7. Improve ONE problem at a time, re-run the table after each change.
  Green = keep going. Red = I just found my next task.

## PART 4: How to understand code I don't understand

  1. Read the CLASS NAME and its annotations first
     (@Service, @Entity, @RestController tell its job).
  2. Read only the METHOD NAMES. Ignore the insides.
  3. Pick ONE method. Write: INPUT -> what it does -> OUTPUT.
  4. Take ONE real example and walk through it with pen and paper.
  5. Put a breakpoint (red dot in IntelliJ) on the first line,
     run in Debug mode, press F8 / F7 and WATCH the variables change.
  6. Add log.info("value = {}", value) to see what is happening.
  7. Change ONE small thing, run again, and see what changes.

## PART 5: How to find a Java / Spring tool myself

  Search for WHAT I WANT TO DO, in plain words:
    "java check whole word in string"
    "spring boot read yml into class"
    "java handle null safely"
    "spring data jpa find by two fields"
  In IntelliJ: type a dot after an object and press Ctrl+Space
  to see everything it can do.
  Good places: official Spring docs, Baeldung, Stack Overflow
  (pick answers with many upvotes, then TEST them with my example table).

## PART 6: When I am stuck (do this BEFORE asking anyone)

  1. Read the LAST line of the error message and the first line that
     mentions MY package (com.jobfinder...). That is where it broke.
  2. Copy the error and search it with the word "spring boot".
  3. Shrink the problem: test it with 1 company / 1 job / 1 request.
  4. Check the usual suspects:
       - forgot an annotation (@Service, @Component, @EnableScheduling)
       - forgot to assign the result (spec = spec.and(...))
       - null value
       - typo in a yml key or a field name
       - app not restarted after changing yml
  5. Explain the problem OUT LOUD to a toy or a friend.
     (Most bugs are found while explaining.)

## PART 7: Daily habit

  - Before coding: write the sentence + the example table.
  - After coding: run the table, then Postman, then commit.
  - At night: write 3 lines in a notebook:
        What I built today.
        What confused me.
        What I will do tomorrow.
  - Once a week: close the code and try to re-write one small class
    from memory. Where I get stuck = what I should revise.

## PRACTICE TASKS (no help allowed for 30 minutes)

  1. Add the rule "reject titles that contain 'unpaid'" using only application.yml.
  2. Add a method that counts how many jobs each company has.
  3. Add an endpoint that returns only jobs first seen in the last 2 days.
  Write the sentence + example table first, then code.
 */

package com.project.job_finder_ai.controller;

import com.project.job_finder_ai.dto.JobView;
import com.project.job_finder_ai.service.JobServices;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobServices service;

    @GetMapping
    public List<JobView> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "false") boolean indiaOnly,
            @RequestParam(required = false) Boolean passedFilter,
            @RequestParam(defaultValue = "20") int limit) {
        return service.search(companyId, keywords, location, department, indiaOnly,passedFilter, limit);
    }

}