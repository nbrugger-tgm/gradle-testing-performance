 ## Gradle testing performance

The idea of this repository is to test the performance of Gradle and other technologies in different scenarios (for unit and integration tests)!
The goal is to provide _reproducable_, _reliable_ and _accurate_ results and metrics that can be used to make better decitions.

> **WARNING**: Currently the results are NOT reliable when comparing between "ten_tests" and non "ten_tests" results! 
> See [reddit discussion](https://www.reddit.com/r/gradle/comments/154z8we/gradle_the_less_tests_you_execute_the_slower_it/)

### How to reproduce results?
1. Clone this repository `git clone https://github.com/nbrugger-tgm/gradle-testing-performance`
2. `cd gradle-testing-performance`
3. `./gradlew run` (this will take some time (~10 mins atm) since - well it will execute ~200k test combinations)
4. open the json in `build/report.json` it includes all results and also calculated metrics

### Covered scenarios and factors
> This is what is **planned** this is WIP!
> Feel free to contribute new scenarios (especially the ones listed)
> See later on how to contribute and create scenarios!

**Scenarios**
- [x] Unit tests (`./gradlew :unit-only:run`)
- [ ] Integration tests (with pre-running DB)
- [ ] Integration tests (with testcontainers)
- [ ] Integration tests (with liquibase provisioning using a pre-running DB)
   - [ ] small changeset (~10 migrations)
   - [ ] medium changeset (~50 migrations)
   - [ ] large changeset (~200 migrations)
- [ ] Integration tests (with liquibase provisioning using testcontainers)
    - [ ] small changeset (~10 migrations)
- [ ] Mockito tests
- [ ] Micronaut tests
   - [ ] Small context (20 services/components/beans) all used
   - [ ] Medium context (70 services/components/beans) all used
   - [ ] Medium context (70 services/components/beans) 20 used
   - [ ] Large context (400 services/components/beans) all used
   - [ ] Large context (400 services/components/beans) 20 used

**Factors**
- [x] Test class count
  - 1
  - 50
  - 200
- [x] Test method count
  - 1
  - 10
- [x] Gradle submodule count
  - 1
  - 5
  - 20

This is done in order to better understand how the performance is affected by different factors and how they interact with each other.
For example, Does testcontainers starts a new container for every gradle submodule or only once for all of them?
> This also causes a very large number of tests and very high execution time, but it is worth it! It will result in a very
> large data mass that can be analyzed and used to make better decisions!