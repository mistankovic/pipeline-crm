Feature: Closing a deal as won
  A deal cannot be marked CLOSED_WON unless it has a positive value and at
  least one Call or Meeting recorded on that deal. Notes do not count.
  Winning forces probability to 100. Losing forces probability to 0.

  Background:
    Given a sales user "alex" exists
    And a company "Lumen Health" exists
    And "alex" owns a deal "Clinic rollout" at company "Lumen Health"
    And the deal is worth 120000 EUR at 40% probability
    And the deal is in stage NEGOTIATION

  Scenario: Owner wins a deal after a meeting
    Given the deal has a meeting activity "Site visit"
    When "alex" moves the deal to CLOSED_WON
    Then the deal stage is CLOSED_WON
    And the deal probability is 100%

  Scenario: Owner wins a deal after a call
    Given the deal has a call activity "Budget confirmation"
    When "alex" moves the deal to CLOSED_WON
    Then the deal stage is CLOSED_WON
    And the deal probability is 100%

  Scenario: Notes alone do not satisfy the conversation guard
    Given the deal has a note activity "Internal recap"
    When "alex" attempts to move the deal to CLOSED_WON
    Then the change is rejected with code "CLOSE_WON_ACTIVITY"
    And the deal stage is NEGOTIATION

  Scenario: A conversation on a different deal does not count
    Given another deal "Unrelated" owned by "alex" at company "Lumen Health"
    And deal "Unrelated" has a meeting activity "Wrong meeting"
    When "alex" attempts to move the deal "Clinic rollout" to CLOSED_WON
    Then the change is rejected with code "CLOSE_WON_ACTIVITY"

  Scenario: Zero-value deals cannot be won
    Given the deal is worth 0 EUR at 40% probability
    And the deal has a meeting activity "Site visit"
    When "alex" attempts to move the deal to CLOSED_WON
    Then the change is rejected with code "CLOSE_WON_VALUE"

  Scenario: Owner marks a deal lost
    When "alex" moves the deal to CLOSED_LOST
    Then the deal stage is CLOSED_LOST
    And the deal probability is 0%

  Scenario: Losing does not require a conversation or a value
    Given the deal is worth 0 EUR at 15% probability
    When "alex" moves the deal to CLOSED_LOST
    Then the deal stage is CLOSED_LOST
    And the deal probability is 0%
