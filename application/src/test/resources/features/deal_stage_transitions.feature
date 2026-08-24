Feature: Deal stage transitions
  The sales pipeline is a guarded state machine. Happy-path stages cannot be skipped.
  A deal may close as won or lost from any open stage. Terminal stages are terminal.

  Background:
    Given a sales user "owner" owns a deal "Acme expansion" in stage LEAD with value 1000 USD and probability 25
    And the deal has a qualifying meeting activity

  Scenario Outline: allowed happy-path moves
    Given the deal is in stage <from>
    When the owner moves the deal to <to>
    Then the deal stage is <to>

    Examples:
      | from        | to          |
      | LEAD        | QUALIFIED   |
      | QUALIFIED   | PROPOSAL    |
      | PROPOSAL    | NEGOTIATION |

  Scenario Outline: skipping a happy-path stage is rejected
    Given the deal is in stage <from>
    When the owner moves the deal to <to>
    Then the change is rejected as an illegal stage transition
    And the deal stage is still <from>

    Examples:
      | from        | to          |
      | LEAD        | PROPOSAL    |
      | LEAD        | NEGOTIATION |
      | QUALIFIED   | NEGOTIATION |
      | QUALIFIED   | LEAD        |
      | PROPOSAL    | QUALIFIED   |
      | NEGOTIATION | PROPOSAL    |

  Scenario Outline: a deal may close from any open stage
    Given the deal is in stage <from>
    When the owner moves the deal to <closed>
    Then the deal stage is <closed>

    Examples:
      | from        | closed      |
      | LEAD        | CLOSED_WON  |
      | LEAD        | CLOSED_LOST |
      | QUALIFIED   | CLOSED_WON  |
      | QUALIFIED   | CLOSED_LOST |
      | PROPOSAL    | CLOSED_WON  |
      | PROPOSAL    | CLOSED_LOST |
      | NEGOTIATION | CLOSED_WON  |
      | NEGOTIATION | CLOSED_LOST |

  Scenario Outline: a closed deal cannot leave a terminal stage
    Given the deal is in stage <closed>
    When the owner moves the deal to <to>
    Then the change is rejected as an illegal stage transition
    And the deal stage is still <closed>

    Examples:
      | closed      | to          |
      | CLOSED_WON  | LEAD        |
      | CLOSED_WON  | QUALIFIED   |
      | CLOSED_WON  | CLOSED_LOST |
      | CLOSED_WON  | CLOSED_WON  |
      | CLOSED_LOST | LEAD        |
      | CLOSED_LOST | CLOSED_WON  |
      | CLOSED_LOST | CLOSED_LOST |

  Scenario Outline: staying in the same open stage is not a transition
    Given the deal is in stage <stage>
    When the owner moves the deal to <stage>
    Then the change is rejected as an illegal stage transition

    Examples:
      | stage       |
      | LEAD        |
      | QUALIFIED   |
      | PROPOSAL    |
      | NEGOTIATION |
