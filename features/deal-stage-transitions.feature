Feature: Deal stage transitions
  Sales people move deals through the pipeline. Closed deals are terminal.
  Probability is only forced when a deal is closed.

  Background:
    Given a sales user "alex" exists
    And a company "Harbor & Co" exists
    And "alex" owns a deal "Fleet telemetry" at company "Harbor & Co"
    And the deal is worth 50000 USD at 20% probability
    And the deal is in stage LEAD

  Scenario: Owner moves a deal to another open stage
    When "alex" moves the deal to QUALIFIED
    Then the deal stage is QUALIFIED
    And the deal probability is 20%

  Scenario: Owner may skip forward to PROPOSAL
    When "alex" moves the deal to PROPOSAL
    Then the deal stage is PROPOSAL

  Scenario: Owner may move backward
    Given the deal is in stage NEGOTIATION
    When "alex" moves the deal to QUALIFIED
    Then the deal stage is QUALIFIED

  Scenario: Moving to the same stage is rejected
    When "alex" attempts to move the deal to LEAD
    Then the change is rejected with code "ILLEGAL_STAGE_TRANSITION"

  Scenario: A closed-lost deal cannot be reopened
    Given the deal is in stage CLOSED_LOST
    When "alex" attempts to move the deal to LEAD
    Then the change is rejected with code "ILLEGAL_STAGE_TRANSITION"

  Scenario: A closed-won deal cannot be moved
    Given the deal has a meeting activity "Kickoff"
    And the deal is in stage CLOSED_WON
    When "alex" attempts to move the deal to NEGOTIATION
    Then the change is rejected with code "ILLEGAL_STAGE_TRANSITION"
