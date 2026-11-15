# Specification Guide

Read this when you have to add a new filter to the job search (`JobServices.search()`).

---

## Part 1: What is a Specification?

A Specification is just **one small piece of a WHERE condition, saved in a variable**. That is all.

Think about any shopping app (Amazon, Flipkart). On the left side you have filters: Brand = Nike, Price < 3000, Colour = Black. You can tick any filters you want, and the app shows only the products which match **all** the ticked filters. Each filter is independent. We can tick one, two or all of them.

Our job search is exactly the same. Each filter is one Specification:

| Filter on screen | Specification (in `JobSpecifications`) |
|---|---|
| Only live jobs | `JobSpecifications.active()` |
| Company = OpenAI | `JobSpecifications.company(companyId)` |
| Title has "backend" or "java" | `JobSpecifications.titleContainsAny(keywords)` |
| Department = Engineering | `JobSpecifications.departmentContains(department)` |
| Location = India | `JobSpecifications.locationContainsAny(...)` |

Each Specification knows how to write only its own small WHERE part. Later we join all the ticked parts with AND, and Spring writes the full SQL for us.

---

## Part 2: Why do we use it? Why not normal repository methods?

Normal Spring Data way is to write a method in the repository, like:

```java
findByCompanyId(Long id)
findByCompanyIdAndTitleContaining(Long id, String title)
findByCompanyIdAndTitleContainingAndLocationContaining(...)
```

This works when we have 1 or 2 filters. But our filters are **optional**. User may send only company, or only keywords, or company + location, or all of them. With 5 optional filters there are 2 × 2 × 2 × 2 × 2 = **32 different combinations**, and we would need to write 32 repository methods. Nobody wants that.

With Specification we write **5 small pieces** only, and we join the pieces which the user actually sent. 5 pieces handle all 32 combinations. And when tomorrow we need a 6th filter, we write just 1 more piece, not 32 more methods.

---

## Part 3: Which parts are inside a Specification?

A Specification has one method inside it, which looks like this:

```java
toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb)
```

We almost never write `toPredicate` by hand. We write a lambda instead:

```java
(root, query, cb) -> ... some condition ...
```

These three small things are the only things we need to understand:

1. **`root`** = the **table** we are searching in. Here root means the `Job` table. `root.get("title")` means "the title column of Job".
   **Important:** inside `get("...")` we write the **Java field name** of the Job entity (like `"firstSeenAt"`), **not** the database column name (like `first_seen_at`).

2. **`query`** = the whole query which is being built. We rarely touch it. We use it only for special things like DISTINCT or joins.

3. **`cb`** = `CriteriaBuilder`. It is our toolbox for making conditions. Every SQL word has a method in it:

| SQL | `cb` method |
|---|---|
| `a = b` | `cb.equal(a, b)` |
| `a != b` | `cb.notEqual(a, b)` |
| `a LIKE '%x%'` | `cb.like(a, "%x%")` |
| `LOWER(a)` | `cb.lower(a)` |
| `a > b` | `cb.greaterThan(a, b)` |
| `a >= b` | `cb.greaterThanOrEqualTo(a, b)` |
| `a < b` | `cb.lessThan(a, b)` |
| `a IS NULL` | `cb.isNull(a)` |
| `a IS NOT NULL` | `cb.isNotNull(a)` |
| `a IN (1, 2, 3)` | `a.in(1, 2, 3)` |
| `x AND y` | `cb.and(x, y)` |
| `x OR y` | `cb.or(x, y)` |
| `NOT x` | `cb.not(x)` |

What comes out is called a **Predicate**. A Predicate is just one condition, like `title LIKE '%java%'`.

---

## Part 4: How does a Specification look inside `JobSpecifications`?

> This is the usual shape of such methods. Our real `JobSpecifications` class may be written a little differently, but the idea is the same.

### Example A: simple "equal" filter

```java
public static Specification<Job> company(Long companyId) {
    return (root, query, cb) ->
        cb.equal(root.get("company").get("id"), companyId);
}
```

In SQL this becomes:

```sql
WHERE company_id = 5
```

### Example B: "contains any" filter (many words joined with OR)

```java
public static Specification<Job> titleContainsAny(List<String> keywords) {
    return (root, query, cb) -> {
        List<Predicate> likes = keywords.stream()
            .map(k -> cb.like(cb.lower(root.get("title")),
                              "%" + k.toLowerCase() + "%"))
            .toList();
        return cb.or(likes.toArray(new Predicate[0]));
    };
}
```

For keywords `["backend", "java"]` the SQL becomes:

```sql
WHERE (LOWER(title) LIKE '%backend%' OR LOWER(title) LIKE '%java%')
```

We use `lower()` on both sides so that "Backend", "BACKEND" and "backend" all match. Otherwise a user searching "backend" would miss "Backend Engineer".

---

## Part 5: How do we join many Specifications together?

```java
spec = spec.and(anotherSpec);    // both conditions must be true
spec = spec.or(anotherSpec);     // any one condition can be true
spec = Specification.not(spec);  // reverse the condition
```

Two very important rules:

**Rule 1:** A Specification is **immutable** (it never changes itself). `spec.and(x)` does **not** modify `spec`. It gives back a **new** Specification. So always write `spec = spec.and(x);` and never just `spec.and(x);` (the second one does nothing, the filter is silently lost).

**Rule 2:** Between different filters we use **AND** (company AND keywords AND location). But **inside** one filter we use **OR** (title has "java" OR "backend"). If we mix this up, we get wrong results.

**Why do we start with `JobSpecifications.active()`?** Because we need some starting point to chain the `.and()` calls on. `active()` is a filter which we always want anyway (do not show closed jobs), so it works as a perfect base. Without any base we would have to deal with a null Specification.

---

## Part 6: Real example, step by step

Suppose the job table has these 5 rows (company OpenAI has id = 5):

| id | title | company | location | department | active | firstSeenAt |
|---|---|---|---|---|---|---|
| 1 | Backend Engineer | OpenAI | Bengaluru, India | Engineering | true | 08 Nov |
| 2 | Java Developer | OpenAI | San Francisco | Engineering | true | 07 Nov |
| 3 | Marketing Manager | OpenAI | Mumbai, India | Marketing | true | 06 Nov |
| 4 | Senior Backend Developer | OpenAI | Remote - India | Engineering | false | 01 Nov |
| 5 | Backend Engineer | Stripe | Bengaluru, India | Engineering | true | 09 Nov |

User asks: *"Show me backend or java jobs of OpenAI, only India."* So we call:

```java
search(5, List.of("backend", "java"), null, null, true, 50)
```

The method builds the filter one by one and every filter removes some rows:

| Step | Filter | What it removes |
|---|---|---|
| 1 | `active()` | row 4 (job is closed) |
| 2 | `company(5)` | row 5 (it is Stripe, not OpenAI) |
| 3 | `titleContainsAny(keywords)` | row 3 ("Marketing Manager" has neither "backend" nor "java") |
| 4 | location has India words | row 2 ("San Francisco" is not India) |

**Left with:** row 1 (Backend Engineer, Bengaluru, India)

The SQL which Spring writes for us is roughly:

```sql
SELECT * FROM job j
WHERE j.active = true
  AND j.company_id = 5
  AND (LOWER(j.title) LIKE '%backend%' OR LOWER(j.title) LIKE '%java%')
  AND (LOWER(j.location) LIKE '%india%' OR ...other India words...)
ORDER BY j.first_seen_at DESC
LIMIT 50
```

Notice that we never wrote this SQL. We only said "add this filter if the user has sent it", and Spring did the rest.

**What if the user sends nothing except limit?** Then no `if` block runs, only `active()` stays, and the SQL is simply `WHERE active = true ORDER BY ... LIMIT`. It still works. That is the beauty of this approach.

---

## Part 7: How to add a NEW filter (3 easy steps)

Example: we want *"show only jobs which we saw in the last 7 days"*.

**Step 1:** Write a new small piece inside `JobSpecifications`.

```java
public static Specification<Job> seenAfter(LocalDateTime fromDate) {
    return (root, query, cb) ->
        cb.greaterThanOrEqualTo(root.get("firstSeenAt"), fromDate);
}
```

**Step 2:** Take the new value as a parameter in `search()`, for example `Integer lastDays`.

**Step 3:** Add one more `if` block in `search()`. Always check for null first, because the filter is optional.

```java
if (lastDays != null) {
    spec = spec.and(JobSpecifications.seenAfter(
        LocalDateTime.now().minusDays(lastDays)));
}
```

That is all. Nothing else in the class has to change.

**Another example:** "exclude jobs which have the word 'intern' in the title".

```java
public static Specification<Job> titleNotContains(String word) {
    return (root, query, cb) -> cb.not(
        cb.like(cb.lower(root.get("title")), "%" + word.toLowerCase() + "%"));
}
```

---

## Part 8: Small mistakes to avoid

- **Forgetting to reassign:** `spec.and(x);` must be `spec = spec.and(x);`
- **Wrong field name in `root.get("...")`.** It must be the Java field name of the entity. A typo here will **not** fail at compile time, it fails only when the search is actually run.
- **Forgetting the null / empty check** before adding a filter. Without the check, an optional filter becomes a compulsory filter and the user gets zero results.
- **Forgetting `lower()` in text search**, which makes the search case sensitive.
- **`JobRepository` must extend `JpaSpecificationExecutor<Job>`**, otherwise `jobRepository.findAll(spec, page)` will not exist.