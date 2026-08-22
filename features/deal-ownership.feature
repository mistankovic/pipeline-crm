Feature: Deal ownership and authorization
  Only the deal owner or a MANAGER may change the stage of a deal.
  Managers may reassign ownership. Sales people may not.

  Background:
    Given a sales user "alex" exists
    And a sales user "blair" exists
    And a manager user "casey" exists
    And a company "Atlas Freight" exists
    And "alex" owns a deal "Yard scanners" at company "Atlas Freight"
    And the deal is worth 80000 USD at 30% probability
    And the deal is in stage QUALIFIED
    And the deal has a meeting activity "Ops walkthrough"

  Scenario: A different sales user cannot change stage
    When "blair" attempts to move the deal to PROPOSAL
    Then the change is rejected with code "NOT_AUTHORIZED"
    And the deal stage is QUALIFIED

  Scenario: A manager may change someone else's deal
    When "casey" moves the deal to PROPOSAL
    Then the deal stage is PROPOSAL

  Scenario: A manager may close someone else's deal as won
    When "casey" moves the deal to CLOSED_WON
    Then the deal stage is CLOSED_WON
    And the deal probability is 100%

  Scenario: The owner may change their own deal
    When "alex" moves the deal to PROPOSAL
    Then the deal stage is PROPOSAL

  Scenario: A sales user cannot reassign a deal
    When "alex" attempts to reassign the deal to "blair"
    Then the change is rejected with code "NOT_AUTHORIZED"
    And the deal owner is "alex"

  Scenario: A manager may reassign a deal
    When "casey" reassigns the deal to "blair"
    Then the deal owner is "blair"

  Scenario: Reassignment is refused on a closed deal
    Given the deal is in stage CLOSED_LOST
    When "casey" attempts to reassign the deal to "blair"
    Then the change is rejected with code "TERMINAL_DEAL"
