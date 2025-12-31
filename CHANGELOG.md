# Changelog

## Version 1
- Initial release of the thread-safe expiring LRU cache implementation.
- Provides O(1) get/put operations guarded by a lock for thread safety.
- Supports per-entry expiration, manual cleanup of expired entries, and basic test harness.
