#!/usr/bin/env bash
# Git Bash / MSYS: runs the PowerShell runner so you get the same env + Maven output.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PS1_FILE="${SCRIPT_DIR}/run-notification.ps1"

echo "Launching run-notification.ps1 via PowerShell..."

if command -v pwsh >/dev/null 2>&1; then
  pwsh -NoProfile -ExecutionPolicy Bypass -File "$PS1_FILE"
elif command -v powershell.exe >/dev/null 2>&1; then
  # Convert to Windows path when possible (Git Bash)
  if command -v cygpath >/dev/null 2>&1; then
    WIN_PATH="$(cygpath -w "$PS1_FILE")"
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$WIN_PATH"
  else
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$PS1_FILE"
  fi
else
  echo "Error: Install PowerShell (pwsh or powershell.exe) to run this script."
  exit 1
fi
