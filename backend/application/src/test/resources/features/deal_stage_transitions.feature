Feature: Moving a deal through the pipeline

  A deal travels LEAD, QUALIFIED, PROPOSAL, NEGOTIATION and finally closes as won or lost.
  It advances one stage at a time so that the pipeline report means something. It may be
  lost at any point, because a customer can stop answering the phone at any point.
  Once closed, a deal is history: nothing moves it again.

  Background:
    Given a salesperson "Sam"
    And a company "Acme"

  Scenario: A new deal starts as a lead
    When Sam creates a deal "Acme renewal" for "Acme" worth 10000 EUR at 50% probability
    Then the deal "Acme renewal" is in stage LEAD

  Scenario Outline: A deal advances one stage at a time
    Given Sam owns a deal "Acme renewal" in stage <from> worth 10000 EUR
    When Sam moves "Acme renewal" to <to>
    Then the deal "Acme renewal" is in stage <to>

    Examples:
      | from        | to          |
      | LEAD        | QUALIFIED   |
      | QUALIFIED   | PROPOSAL    |
      | PROPOSAL    | NEGOTIATION |

  Scenario Outline: A deal may not skip a stage
    Given Sam owns a deal "Acme renewal" in stage <from> worth 10000 EUR
    When Sam tries to move "Acme renewal" to <to>
    Then the move is rejected because the transition is not allowed
    And the deal "Acme renewal" is in stage <from>

    Examples:
      | from      | to          |
      | LEAD      | PROPOSAL    |
      | LEAD      | NEGOTIATION |
      | QUALIFIED | NEGOTIATION |

  Scenario Outline: A deal never moves backwards
    Given Sam owns a deal "Acme renewal" in stage <from> worth 10000 EUR
    When Sam tries to move "Acme renewal" to <to>
    Then the move is rejected because the transition is not allowed

    Examples:
      | from        | to        |
      | QUALIFIED   | LEAD      |
      | PROPOSAL    | QUALIFIED |
      | NEGOTIATION | PROPOSAL  |

  Scenario Outline: A deal can be lost from any open stage
    Given Sam owns a deal "Acme renewal" in stage <from> worth 10000 EUR
    When Sam moves "Acme renewal" to CLOSED_LOST
    Then the deal "Acme renewal" is in stage CLOSED_LOST

    Examples:
      | from        |
      | LEAD        |
      | QUALIFIED   |
      | PROPOSAL    |
      | NEGOTIATION |

  Scenario: A won deal cannot be reopened
    Given Sam owns a won deal "Acme renewal" worth 10000 EUR
    When Sam tries to move "Acme renewal" to NEGOTIATION
    Then the move is rejected because the transition is not allowed
    And the deal "Acme renewal" is in stage CLOSED_WON

  Scenario: A lost deal cannot be revived
    Given Sam owns a deal "Acme renewal" in stage CLOSED_LOST worth 10000 EUR
    When Sam tries to move "Acme renewal" to QUALIFIED
    Then the move is rejected because the transition is not allowed

  Scenario: A lost deal cannot be lost twice
    Given Sam owns a deal "Acme renewal" in stage CLOSED_LOST worth 10000 EUR
    When Sam tries to move "Acme renewal" to CLOSED_LOST
    Then the move is rejected because the transition is not allowed
