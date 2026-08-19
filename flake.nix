{
  description = "Phaze — OpenSubsonic Android music player dev shell";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
          config.android_sdk.accept_license = true;
        };

        # Android SDK composed with the exact API level and build tools
        # the project targets. If you manage the SDK via Android Studio
        # instead, comment this block out and point ANDROID_SDK_ROOT
        # to your Studio-managed path in the shellHook below.
        androidComposition = pkgs.androidenv.composeAndroidPackages {
          cmdLineToolsVersion = "11.0";
          platformVersions = [ "34" ];
          buildToolsVersions = [ "34.0.0" ];
          includeEmulator = false;
          includeSources = false;
          includeSystemImages = false;
          systemImageTypes = [ ];
          abiVersions = [ ];
          cmakeVersions = [ ];
          ndkVersions = [ ];
          includeNDK = false;
          useGoogleAPIs = false;
          useGoogleTVAddOns = false;
        };

        androidSdk = androidComposition.androidsdk;
      in
      {
        devShells.default = pkgs.mkShell {
          name = "phaze-dev";

          buildInputs = [
            androidSdk
            pkgs.android-tools
            pkgs.jdk17
            pkgs.gradle
            pkgs.kotlin
            pkgs.git
            pkgs.pre-commit
            pkgs.prettierd
          ];

          shellHook = ''
            						export ANDROID_SDK_ROOT="${androidSdk}/libexec/android-sdk"
            						export ANDROID_HOME="$ANDROID_SDK_ROOT"
            						export JAVA_HOME="${pkgs.jdk17.home}"
            						export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/34.0.0/aapt2"

            						echo "Phaze dev shell ready."
            						echo "  Android SDK: $ANDROID_SDK_ROOT"
            						echo "  JAVA_HOME:   $JAVA_HOME"
            						echo ""
            						echo "Quick start:"
            						echo "  ./gradlew assembleDebug"
            						echo "  ./gradlew connectedCheck   # needs a device / emulator"
            						echo ""

            						# Install pre-commit hooks if not already present
            						if [ -f .pre-commit-config.yaml ] && [ ! -d .git/hooks/pre-commit ]; then
            							echo "Installing pre-commit hooks..."
            							pre-commit install
            						fi
          '';
        };

        # Optional: run `nix fmt` to format this file and other .nix files
        formatter = pkgs.nixpkgs-fmt;
      }
    );
}
