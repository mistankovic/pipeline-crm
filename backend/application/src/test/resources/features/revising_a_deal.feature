Feature: Revising an open deal

  A deal's value and its probability are working numbers while the deal is open, and they
  belong to the person who owns it. A closed deal's numbers are history and stop being
  editable, because a report that can be rewritten after the fact is not a report.

  Background:
    Given a salesperson "Sam"
    And a salesperson "Robin"
    And a manager "Mo"
    And a company "Acme"

  Scenario: The owner can reprice an open deal
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Sam reprices "Acme renewal" to 20000 EUR
    Then the deal "Acme renewal" is worth 20000 EUR

  Scenario: The owner can reweight an open deal
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Sam reweights "Acme renewal" to 40%
    Then the deal "Acme renewal" has probability 40

  Scenario: A manager can revise somebody else's deal
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Mo reprices "Acme renewal" to 20000 EUR
    Then the deal "Acme renewal" is worth 20000 EUR

  Scenario: Another salesperson cannot quietly zero out a rival's deal
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Robin tries to reprice "Acme renewal" to 0 EUR
    Then the change is rejected because the user has no authority over the deal
    And the deal "Acme renewal" is worth 10000 EUR

  Scenario: Another salesperson cannot quietly drop a rival's probability either
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Robin tries to reweight "Acme renewal" to 0%
    Then the change is rejected because the user has no authority over the deal
    And the deal "Acme renewal" has probability 80

  Scenario: A closed deal's value can no longer be edited
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    And Sam moves "Acme renewal" to CLOSED_LOST
    When Sam tries to reprice "Acme renewal" to 20000 EUR
    Then the change is rejected because the deal is closed

  Scenario: A closed deal's probability can no longer be edited
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    And Sam moves "Acme renewal" to CLOSED_LOST
    When Sam tries to reweight "Acme renewal" to 90%
    Then the change is rejected because the deal is closed

  Scenario: Repricing states the currency rather than inheriting it
    Given Sam owns a deal "Acme renewal" in stage PROPOSAL worth 10000 EUR at 80% probability
    When Sam reprices "Acme renewal" to 15000 USD
    Then the deal "Acme renewal" is worth 15000 USD

  Scenario: A deal that does not exist cannot be repriced
    When Sam tries to reprice an unknown deal to 100 EUR
    Then the change is rejected because the deal does not exist
