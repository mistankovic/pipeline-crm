Feature: Recording what happened

  Every call, meeting and note is recorded against exactly one deal or one contact, by the
  person who recorded it. The timeline of a deal is what later justifies closing it.

  Background:
    Given a salesperson "Sam"
    And a company "Acme"
    And a contact "Cara" at "Acme"

  Scenario: A call is logged against a deal
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Sam logs a CALL "discussed renewal terms" against the deal "Acme renewal"
    Then the timeline of "Acme renewal" has 1 entry
    And the timeline of "Acme renewal" contains a CALL

  Scenario: A note is logged against a contact
    When Sam logs a NOTE "prefers email" against the contact "Cara"
    Then the timeline of the contact "Cara" has 1 entry

  Scenario: An activity against a contact does not appear on a deal timeline
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Sam logs a MEETING "introductions" against the contact "Cara"
    Then the timeline of "Acme renewal" has 0 entries

  Scenario: An activity against a contact is not evidence for winning a deal
    Given Sam owns a deal "Acme renewal" in stage NEGOTIATION worth 10000 EUR
    And Sam logs a MEETING "introductions" against the contact "Cara"
    When Sam tries to move "Acme renewal" to CLOSED_WON
    Then the move is rejected because the deal has not earned a win

  Scenario: Activities cannot be logged against a deal that does not exist
    When Sam tries to log a CALL against an unknown deal
    Then the activity is rejected because the deal does not exist

  Scenario: The timeline records who logged the activity
    Given Sam owns a deal "Acme renewal" in stage LEAD worth 10000 EUR
    When Sam logs a CALL "discussed renewal terms" against the deal "Acme renewal"
    Then the only entry on the timeline of "Acme renewal" was recorded by Sam
