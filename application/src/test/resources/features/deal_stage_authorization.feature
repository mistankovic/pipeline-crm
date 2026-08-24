Feature: Who may change deal stage
  Only the deal owner or a user with role MANAGER may change stage.
  Another SALES user is rejected. The rule is enforced in the domain, not the UI.

  Background:
    Given a sales user "owner" owns a deal "Acme expansion" in stage LEAD with value 1000 USD and probability 25
    And a sales user "peer" exists
    And a manager user "boss" exists

  Scenario: owner may change stage
    When "owner" moves the deal to QUALIFIED
    Then the deal stage is QUALIFIED

  Scenario: manager may change someone else's deal
    When "boss" moves the deal to QUALIFIED
    Then the deal stage is QUALIFIED

  Scenario: another sales user may not change stage
    When "peer" moves the deal to QUALIFIED
    Then the change is rejected as unauthorized
    And the deal stage is still LEAD
