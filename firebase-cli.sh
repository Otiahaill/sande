#!/bin/sh
# Firebase CLI wrapper for the sande project.
# The shared-storage workspace cannot create symlinks, so npm's .bin shim is
# unavailable. This wrapper calls the CLI entry point directly with node.
DIR="$(cd "$(dirname "$0")" && pwd)"
exec node "$DIR/../firebase-tools-local/node_modules/firebase-tools/lib/bin/firebase.js" "$@"
