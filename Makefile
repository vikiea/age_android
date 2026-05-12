APP_APK  := app/build/outputs/apk/debug/app-debug.apk
ENGINE_SRC := age-engine/src/main/go
ENGINE_AAR := age-engine/libs/age-engine.aar
GOMOBILE := $(HOME)/go/bin/gomobile
ADB_DEVICE ?= 172.16.76.232:44867

# ADB target flag (empty = default device, set to ip:port for TCP)
ADB_FLAG := $(if $(ADB_DEVICE),-s $(ADB_DEVICE),)

# ── Go engine ──────────────────────────────────────────────
.PHONY: engine
engine: ## Rebuild Go engine AAR via gomobile
	cd $(ENGINE_SRC) && $(GOMOBILE) bind \
		-target=android -androidapi=26 \
		-o $(CURDIR)/$(ENGINE_AAR) \
		-javapkg=com.age.engine .

# ── App build ──────────────────────────────────────────────
.PHONY: build
build: ## Build debug APK (incremental)
	./gradlew :app:assembleDebug

.PHONY: clean
clean: ## Clean build artifacts
	./gradlew :app:clean

.PHONY: rebuild
rebuild: clean build ## Clean + build

.PHONY: full
full: engine rebuild ## Rebuild engine + clean build app

# ── Deploy ─────────────────────────────────────────────────
.PHONY: install
install: ## Install APK to device
	adb $(ADB_FLAG) install -r $(APP_APK)

.PHONY: deploy
deploy: build install ## Build + install

.PHONY: redeploy
redeploy: rebuild install ## Clean build + install

.PHONY: full-deploy
full-deploy: full install ## Engine rebuild + clean build + install

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
