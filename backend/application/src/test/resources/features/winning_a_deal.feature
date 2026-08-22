Feature: Winning a deal

  A win is a commercial commitment, so the system asks for evidence before it records one:
  the deal must be worth something, and somebody must actually have spoken to the customer
  about this deal. A note we wrote to ourselves is not evidence. A call about a different
  deal is not evidence either.

  Background:
    Given a salesperson "Sam"
    And a company "Acme"

  Scenario: A deal in negotiation with value and a meeting can be won
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    And a MEETING has been logged against "Acme renewal"
    When Sam moves "Acme renewal" to CLOSED_WON
    Then the deal "Acme renewal" is in stage CLOSED_WON

  Scenario: A call is enough evidence
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    And a CALL has been logged against "Acme renewal"
    When Sam moves "Acme renewal" to CLOSED_WON
    Then the deal "Acme renewal" is in stage CLOSED_WON

  Scenario: A deal worth nothing cannot be won
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 0 EUR
    And a MEETING has been logged against "Acme renewal"
    When Sam tries to move "Acme renewal" to CLOSED_WON
    Then the move is rejected because the deal has not earned a win
    And the deal "Acme renewal" is in stage NEGOTIATION

  Scenario: A deal with only notes cannot be won
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    And a NOTE has been logged against "Acme renewal"
    When Sam tries to move "Acme renewal" to CLOSED_WON
    Then the move is rejected because the deal has not earned a win
    And the deal "Acme renewal" is in stage NEGOTIATION

  Scenario: A deal nobody has touched cannot be won
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    When Sam tries to move "Acme renewal" to CLOSED_WON
    Then the move is rejected because the deal has not earned a win

  Scenario: A meeting about another deal is not evidence for this one
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    And Sam owns a deal "Acme expansion" in stage NEGOTIATION worth 5000 EUR
    And a MEETING has been logged against "Acme expansion"
    When Sam tries to move "Acme renewal" to CLOSED_WON
    Then the move is rejected because the deal has not earned a win

  Scenario: A deal worth nothing may still be lost
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 0 EUR
    When Sam moves "Acme renewal" to CLOSED_LOST
    Then the deal "Acme renewal" is in stage CLOSED_LOST
