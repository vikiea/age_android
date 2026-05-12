APP_DEBUG_APK   := app/build/outputs/apk/debug/app-debug.apk
APP_RELEASE_DIR := app/build/outputs/apk/release
ENGINE_SRC := age-engine/src/main/go
ENGINE_AAR := age-engine/libs/age-engine.aar
ENGINE_LIBS := age-engine/libs
ENGINE_JNI := age-engine/src/main/jniLibs
GOMOBILE := $(HOME)/go/bin/gomobile
ADB_DEVICE ?= 172.16.76.232:44863

# ADB target flag (empty = default device, set to ip:port for TCP)
ADB_FLAG := $(if $(ADB_DEVICE),-s $(ADB_DEVICE),)

# ── Go engine ──────────────────────────────────────────────
.PHONY: engine
engine: ## Rebuild Go engine AAR via gomobile
	cd $(ENGINE_SRC) && $(GOMOBILE) bind \
		-target=android -androidapi=26 \
		-o $(CURDIR)/$(ENGINE_AAR) \
		-javapkg=com.age.engine .
	@# Extract AAR → classes.jar + jniLibs (release build requires this)
	@cd /tmp && rm -rf _aar_extract && mkdir _aar_extract && cd _aar_extract \
		&& unzip -q $(CURDIR)/$(ENGINE_AAR)
	@cp /tmp/_aar_extract/classes.jar $(ENGINE_LIBS)/age-engine-classes.jar
	@rm -rf $(ENGINE_JNI)
	@cp -r /tmp/_aar_extract/jni $(ENGINE_JNI)
	@rm -rf /tmp/_aar_extract
	@echo "✓ Engine AAR built and extracted"

# ── Debug build ────────────────────────────────────────────
.PHONY: build
build: ## Build debug APK (incremental)
	./gradlew :app:assembleDebug

.PHONY: clean
clean: ## Clean build artifacts
	./gradlew :app:clean

.PHONY: rebuild
rebuild: clean build ## Clean + build debug

.PHONY: full
full: engine rebuild ## Rebuild engine + clean build debug

# ── Release build ──────────────────────────────────────────
.PHONY: release
release: ## Build release APK (signed, R8 optimized)
	./gradlew :app:assembleRelease
	@echo "✓ Release APKs:"
	@ls -lh $(APP_RELEASE_DIR)/*.apk

.PHONY: release-arm64
release-arm64: ## Build release APK for arm64-v8a only
	./gradlew :app:assembleRelease
	@echo "✓ arm64: $$(ls -lh $(APP_RELEASE_DIR)/app-arm64-v8a-release.apk | awk '{print $$5}')"

.PHONY: full-release
full-release: engine release ## Rebuild engine + release build

# ── Deploy ─────────────────────────────────────────────────
.PHONY: install
install: ## Install debug APK to device
	adb $(ADB_FLAG) install -r $(APP_DEBUG_APK)

.PHONY: install-release
install-release: ## Install release APK (arm64) to device
	adb $(ADB_FLAG) install -r $(APP_RELEASE_DIR)/app-arm64-v8a-release.apk

.PHONY: deploy
deploy: build install ## Build debug + install

.PHONY: deploy-release
deploy-release: release install-release ## Build release + install

.PHONY: full-deploy
full-deploy: full install ## Engine + debug build + install

# ── Device ─────────────────────────────────────────────────
.PHONY: connect
connect: ## Connect to device via TCP (set ADB_DEVICE=ip:port)
	adb connect $(ADB_DEVICE)

.PHONY: devices
devices: ## List connected ADB devices
	adb devices

# ── Help ───────────────────────────────────────────────────
.PHONY: help
help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.DEFAULT_GOAL := help
