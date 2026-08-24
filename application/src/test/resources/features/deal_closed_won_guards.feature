Feature: Closed-won guards
  A deal cannot be marked CLOSED_WON unless its value is strictly greater than zero
  and it has at least one activity of type Call or Meeting linked to that deal.
  Notes do not count. Activities linked to a contact or to a different deal do not count.

  Background:
    Given a sales user "owner" owns a deal "Acme expansion" in stage LEAD with value 1000 USD and probability 25

  Scenario: meeting on the deal allows closed-won
    Given the deal has a meeting activity
    When the owner moves the deal to CLOSED_WON
    Then the deal stage is CLOSED_WON

  Scenario: call on the deal allows closed-won
    Given the deal has a call activity
    When the owner moves the deal to CLOSED_WON
    Then the deal stage is CLOSED_WON

  Scenario: a note on the deal is not enough
    Given the deal has a note activity
    When the owner moves the deal to CLOSED_WON
    Then the change is rejected because the deal is not winnable
    And the deal stage is still LEAD

  Scenario: no activities is not enough
    When the owner moves the deal to CLOSED_WON
    Then the change is rejected because the deal is not winnable

  Scenario: a meeting on a contact does not qualify the deal
    Given a meeting activity exists on a contact of the same company
    When the owner moves the deal to CLOSED_WON
    Then the change is rejected because the deal is not winnable

  Scenario: a meeting on a different deal does not qualify
    Given a meeting activity exists on a different deal
    When the owner moves the deal to CLOSED_WON
    Then the change is rejected because the deal is not winnable

  Scenario: zero value cannot be won even with a meeting
    Given the deal value is 0.00 USD
    And the deal has a meeting activity
    When the owner moves the deal to CLOSED_WON
    Then the change is rejected because the deal is not winnable

  Scenario: closed-lost does not require value or activity
    When the owner moves the deal to CLOSED_LOST
    Then the deal stage is CLOSED_LOST
