module age-engine

go 1.23

require filippo.io/age v1.2.1

require (
	golang.org/x/crypto v0.24.0 // indirect
	golang.org/x/sys v0.21.0 // indirect
)

// Build with Gomobile:
//   gomobile bind -target=android -o age-engine.aar ./age-engine/src/main/go
// Requires: go, golang.org/x/mobile/cmd/gomobile, golang.org/x/mobile/cmd/gobind
