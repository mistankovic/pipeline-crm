Feature: Activities
  An activity is a Note, Call, or Meeting. It must be linked to a deal
  or a contact. Empty bodies are rejected.

  Background:
    Given a sales user "alex" exists
    And a company "Harbor & Co" exists
    And a contact "Mina Cole" at company "Harbor & Co"
    And "alex" owns a deal "Fleet telemetry" at company "Harbor & Co"

  Scenario: Log a meeting on a deal
    When "alex" logs a meeting "Kickoff" on deal "Fleet telemetry"
    Then deal "Fleet telemetry" has 1 conversation activity

  Scenario: Log a note on a contact
    When "alex" logs a note "Met at conference" on contact "Mina Cole"
    Then contact "Mina Cole" has 1 activity

  Scenario: An activity with no deal and no contact is rejected
    When "alex" attempts to log a call "Orphan" with no target
    Then the change is rejected with code "ACTIVITY_TARGET_REQUIRED"

  Scenario: An empty body is rejected
    When "alex" attempts to log a note "" on deal "Fleet telemetry"
    Then the change is rejected with code "ACTIVITY_BODY_REQUIRED"
