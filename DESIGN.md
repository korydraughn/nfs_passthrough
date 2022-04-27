# System Design Overview

Date Created: 2022-04-27

Tools Involved:
- Java 17
- Maven

Expected Deployment:
- Runnable JAR file
- Can be packaged as a DEB package
- Can be hosted in Maven repository

Two configuration files:
- ID Mapping file:
    - Maps IP-ranges to local user (groups?)
    - Potential filename:
        - id_mapping.json
        - id_mapping.yml
        - ip_to_user_mapping.json
- NFS Server:
    - File containing any information needed to start the server
    - Potential filename:
        - server.json

The server will expose one socket-based endpoint that allows client applications to trigger different behavior within the NFS server:
- Reload the ID Mapping config file
- Set new ID Mapping information
    - Overwrites the existing ID Mapping file

NFS Server Structure:
- Background thread that runs a small socket-based server
    - Handles ID Mapping specific requests
    - Requests are processed synchronously and serially
- Passthrough VFS implementation
    - Handles incoming requests triggered by commands such as `ls` and `cp`
    - Uses the ID Mapping information to resolve IPs to a specific user (groups?)
        - TODO: Determine the type of error to return if no mapping exists
    - Carries out user operations by making appropriate calls to OS.

### Additional Investigation
- Should configuration files sit beside each other?
- Should this information live in memory rather than files on disk?
- Investigate additional caches for performance.
    - Cache eviction options, etc.
