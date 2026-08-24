Feature: Probability is forced when a deal closes
  Moving to CLOSED_WON sets probability to 100.
  Moving to CLOSED_LOST sets probability to 0.
  Probability cannot be edited on a terminal deal.

  Background:
    Given a sales user "owner" owns a deal "Acme expansion" in stage LEAD with value 1000 USD and probability 25
    And the deal has a qualifying meeting activity

  Scenario: closed-won forces 100 regardless of previous probability
    When the owner moves the deal to CLOSED_WON
    Then the deal probability is 100

  Scenario: closed-lost forces 0 regardless of previous probability
    When the owner moves the deal to CLOSED_LOST
    Then the deal probability is 0

  Scenario: probability is locked after closed-lost
    Given the owner moved the deal to CLOSED_LOST
    When the owner changes the deal probability to 40
    Then the change is rejected because the deal is closed
    And the deal probability is 0

  Scenario: probability and value are locked after closed-won
    Given the owner moved the deal to CLOSED_WON
    When the owner changes the deal probability to 40
    Then the change is rejected because the deal is closed
    And the deal probability is 100
    When the owner changes the deal value to 1.00 USD
    Then the change is rejected because the deal is closed
