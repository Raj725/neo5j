# Neo5j Kernel

This module, for historical reasons, contains multiple important components of Neo5j:

 - The embedded Java API
    - org.neo5j.graphdb
 - The embedded Java API implementation
    - org.neo5j.kernel.coreapi
    - org.neo5j.kernel.core
 - The embedded Traversal Java API
   - org.neo5j.graphdb.traversal
 - The embedded Traversal API implementation
    - org.neo5j.kernel.traversal
 - Batch Import
    - org.neo5j.unsafe.impl.batchimport
 - Batch Inserter (legacy)
    - org.neo5j.unsafe.batchinsert
 - The transaction state building layer (the "Kernel API")
    - org.neo5j.kernel.api
    - org.neo5j.kernel.impl.api
 - The Storage Engine:
    - org.neo5j.kernel.impl.store,
    - org.neo5j.kernel.impl.recovery
    - org.neo5j.kernel.impl.transaction
 - Configuration
    - org.neo5j.kernel.configuration
 - Common utilities
    - org.neo5j.helpers
    - org.neo5j.kernel.impl.util
    - org.neo5j.kernel.lifecycle
    - org.neo5j.kernel.monitoring
 - Locking
    - org.neo5j.kernel.impl.locking
 - Kernel Extensions
    - org.neo5j.kernel.extension
