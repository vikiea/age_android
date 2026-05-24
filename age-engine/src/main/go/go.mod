module age-engine

go 1.25.0

require (
	filippo.io/age v1.3.1
	golang.org/x/mobile v0.0.0-20260508232728-bebd421c7fa8
)

require (
	filippo.io/hpke v0.4.0 // indirect
	golang.org/x/crypto v0.45.0 // indirect
	golang.org/x/mod v0.36.0 // indirect
	golang.org/x/sync v0.20.0 // indirect
	golang.org/x/sys v0.44.0 // indirect
	golang.org/x/tools v0.45.0 // indirect
)

// Build with Gomobile:
//   gomobile bind -target=android -o age-engine.aar ./age-engine/src/main/go
// Requires: go, golang.org/x/mobile/cmd/gomobile, golang.org/x/mobile/cmd/gobind
