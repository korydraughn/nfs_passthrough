# System Design Notes

Last Discussion: 2022-05-23

- Pre-Meeting Information:
    - Discovered that IP information is captured, but isn't exposed to the layers we care about
        - We can see the list of clients, but it isn't clear how to identify which one we should handle in the context of an operation
        - We've sent emails to the nfs4j development team about this. We expect a response before the end of day
        - Possible Solutions:
            - The nfs4j development team provides methods that identify the IP for the current operation
            - Or, we fork the nfs4j project and pass the client information down to the layers we care about
                - If this route is taken, we will try to get the changes into upstream
    - Learned that the ExportFile class provides functionality for rescanning it
        - Server will stand up a small TCP server that supports one or more operations
            - GET_STATUS
                - Not sure what this means yet (just an idea at this time)
            - SET_EXPORT_FILE
                - Replaces/Swaps the contents of the existing exports file
                - Takes a string representing the contents of the exports file
        - ExportFile class is thread-safe
        - If the exports information is identical, perhaps we change the model to pull from a centralized location?
            - Perhaps, a future enhancement

---
Last Discussion: 2022-05-16

- Can the exports file be extended to support non-standard/custon options?
    - No, it more or less follows the standard Linux export format
    - However, implementing the ExportTable API interface does enable this capability
    - Not urgent, so we will investigate this later if deemed important

- Cold starts are okay for now
    - Learned that avoiding this problem requires the following:
        - File handles must be unique
        - File handles must never be reused
        - File handles must uniquely identify a file
    - If implemented correctly, restarting the NFS server after mounting should not require a re-mount
        - The mount point should still be functional

- Goal for next meeting:
    - Implement test server that demonstrates mapping of IP address to a specific user

---
Last Discussion: 2022-05-02

- Use TCP/IP socket for control
    - Authentication (can be handled later)
    - SSL endpoint with a pre-shared key (can be handled later)
    - Could use allowlist that defines who is allowed to communicate with the server

- One-to-One Mappings only
    - No IP ranges

- Mappings will use JSON for now
    - Can rely on the JSON parser to catch issues
    - Can look into other formats later (perhaps CSV)

- Potential update frequency for mappings:
    - 1 update per minute
    - 1 update every 5 minutes
    - Can see updates every 10 seconds

- Does this require a server.json configuration file?
    - Not at this time. We can pass the port number to listen on via the command line.

- TODO: Investigate extending the exports file
    - Does the framework support this?

---
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
- Should configuration information live in memory rather than files on disk?
- Investigate additional caches for performance.
    - Cache eviction options, etc.
