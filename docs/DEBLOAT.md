# Optional package cleanup

Quiet TV does not require debloating. Home replacement, media access and MK-IV selection have separate setup and rollback paths.

`scripts/tv.py disable <package>` accepts only a small explicit allowlist of optional entertainment apps. Before changing a package, it verifies the exact configured TV, reads the user-0 enabled state, and records an appropriate `enable` or `default-state` rollback. It then checks that the package is disabled and appends the restore command to ignored `logs/restore-debloat.sh`.

Disabling for user 0 retains the installed APK and app data. It is different from `pm uninstall --user 0`, which can remove that user’s data, and neither operation removes a factory APK from the read-only system partition.

Do not treat this allowlist as a recommended removal list. Keep apps/services you use, and never substitute guessed system package names. The script does not disable Google Play services, TV input services, HDMI/CEC services or OxygenPlay through this optional-app path.

For Home restoration, use `python3 scripts/tv.py restore-home` as described in [Setup](SETUP.md). For a package disabled through the optional-app helper, use its recorded restore command after re-verifying the same TV. Device inventories and restore logs are private local files, not public project documentation.
