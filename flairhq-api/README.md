# flairhq-api
FlairHQ API

Notes:
- User session is endless (10 year cookie max age) but moderator permissions are only loaded on login, so users need to log out and back in to gain relevant perms

For key based authentication, need to do the following
Add to secrets.properties:
    service_api_key=<your-long-random-key>
The calling application then sends:
    Authorization: Bearer <your-long-random-key>

New FlairHQ!
* Features (new marked with !!)
    - Profile
        Add, edit, remove trade references on own profile
        Apply for trade flair
        Edit flair text
        Edit profile (intro and fcs)
        !! Link to Reddit profile
    - Search for users or references
    - !! Pending Reciprocal Reference
        New section to show users trades that were approved where they are the other user and they don't have this trade on their profile. It pre-fills the add reference modal with the info from the other trade. When submitted, it will utilize te date of the original ref submitted (helps with ensuring no duplicates as there are some weird permalink issues here). STILL IN PROGRESS
    - !! Progress for specific flairs when applying
        Example, if a user has logged 15 references, it would show 15 / 10 for PokeBall and allow them to apply but also them they are at 15 / 20 for Premier Ball
    - !! Theme switching between light (PokeBall) and dark (Dark Pokedex) themes from user menu
    - !! Show Reddit avatar on user menu and their Profile page

* Moderator Features (new marked with !!)
    - Approve or remove trade references
    - Search event logs and modmail archive
    - Review Flair Applications
    - View Banlist
    - Ban User
    - View Event Log
    - !! Shows progress of Approved Trades for the Flair
        Example, if a user has logged 15 references but hasn't gotten any approved yet and applies for PokeBall flair, it would show 0 / 10 on the flair app
    - !! Ban User button on their FHQ profile
    - !! Reject trade references
        This feature allows Moderators to psuedo remove any trade reference that we think should never be included. If a reference is Rejected, it is only visible by Moderators and the User that submitted it as owner of the profile in its own section. It cannot be edited or changed out of this state. It also has a small number of pre-selectable reasons or freeform text.
    - !! Must Fix trade references
        This feature allows Moderators to tag refs that need to be fixed by the user. Moderators are required to add a reason to this status. These will auto-fill on Deny App modmails. The reason for must fix shows on hover of the status on their profile.
    - !! View as User functionality
        Lets a Moderator browse the site with the permissions of a user.

* Technical Features
    - Vue.js frontend with a Spring Boot java backend
    - Backend API provides support for integrating other things (like our Discord Bot) with FHQ
        Will talk about what we want to do here later
    - Better URL parsing and matching for permalinks (will fix those pesky mobile share links)
        This will help with matching reciprocal trades better
    

* Features built but wondering if we still want/need
    - Public comments on user profiles
    - Mod Notes (add and view) on user profiles
    - Edit Flairs UI (are these ever going to change at this point?!)

* Removed Features
    - Support for adding SVExchange references
    - Support for applying to SVExchange flairs
    - References to SVExchange across the site, apart from SVEx flairs users already have