#!/usr/bin/env bash
# Phaze — minimal gradle wrapper (delegates to system gradle from nix shell)
exec gradle "$@"
