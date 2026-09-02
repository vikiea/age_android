package ageengine

import (
	"bytes"
	"errors"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
)

func TestOfficialAgePostQuantumInteroperability(t *testing.T) {
	agePath, ageErr := exec.LookPath("age")
	inspectPath, inspectErr := exec.LookPath("age-inspect")
	if ageErr != nil || inspectErr != nil {
		t.Skip("official age v1.3.1 and age-inspect are required for interoperability verification")
	}

	pair, err := GenerateKeyPair("")
	if err != nil {
		t.Fatal(err)
	}
	plaintext := []byte("Age Android v5 official interoperability")
	dir := t.TempDir()
	identityPath := filepath.Join(dir, "identity.txt")
	if err := os.WriteFile(identityPath, []byte(pair.PrivateKey+"\n"), 0600); err != nil {
		t.Fatal(err)
	}

	engineCiphertext, err := EncryptWithRecipient(plaintext, pair.PublicKey)
	if err != nil {
		t.Fatal(err)
	}
	decrypt := exec.Command(agePath, "--decrypt", "--identity", identityPath)
	decrypt.Stdin = bytes.NewReader(engineCiphertext)
	decrypted, err := decrypt.Output()
	if err != nil {
		t.Fatalf("official age decrypt: %v", err)
	}
	if !bytes.Equal(decrypted, plaintext) {
		t.Fatal("official age decrypt did not reproduce the plaintext")
	}

	encrypt := exec.Command(agePath, "--recipient", pair.PublicKey)
	encrypt.Stdin = bytes.NewReader(plaintext)
	officialCiphertext, err := encrypt.Output()
	if err != nil {
		t.Fatalf("official age encrypt: %v", err)
	}
	enginePlaintext, err := DecryptWithIdentity(officialCiphertext, pair.PrivateKey)
	if err != nil {
		t.Fatalf("engine decrypt: %v", err)
	}
	if !bytes.Equal(enginePlaintext, plaintext) {
		t.Fatal("engine decrypt did not reproduce the plaintext")
	}

	ciphertextPath := filepath.Join(dir, "pq.age")
	if err := os.WriteFile(ciphertextPath, engineCiphertext, 0600); err != nil {
		t.Fatal(err)
	}
	inspection, err := exec.Command(inspectPath, ciphertextPath).Output()
	if err != nil {
		t.Fatalf("age-inspect: %v", err)
	}
	if !bytes.Contains(inspection, []byte("mlkem768x25519")) {
		t.Fatalf("PQ stanza missing from age-inspect output: %s", inspection)
	}
}

func TestGenerateKeyPairAndRoundTrip(t *testing.T) {
	tests := []struct {
		name          string
		keyType       string
		publicPrefix  string
		privatePrefix string
	}{
		{name: "post quantum default", publicPrefix: "age1pq1", privatePrefix: "AGE-SECRET-KEY-PQ-1"},
		{name: "x25519", keyType: "x25519", publicPrefix: "age1", privatePrefix: "AGE-SECRET-KEY-1"},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pair, err := GenerateKeyPair(test.keyType)
			if err != nil {
				t.Fatal(err)
			}
			if !strings.HasPrefix(pair.PublicKey, test.publicPrefix) {
				t.Fatalf("public key prefix = %q", pair.PublicKey)
			}
			if !strings.HasPrefix(pair.PrivateKey, test.privatePrefix) {
				t.Fatalf("private key prefix = %q", pair.PrivateKey)
			}

			derived, err := RecipientForIdentity(pair.PrivateKey)
			if err != nil {
				t.Fatal(err)
			}
			if derived != pair.PublicKey {
				t.Fatalf("derived recipient mismatch: got %q want %q", derived, pair.PublicKey)
			}

			plaintext := []byte("Age Android v5 interoperability")
			ciphertext, err := EncryptWithRecipient(plaintext, pair.PublicKey)
			if err != nil {
				t.Fatal(err)
			}
			decrypted, err := DecryptWithIdentity(ciphertext, pair.PrivateKey)
			if err != nil {
				t.Fatal(err)
			}
			if string(decrypted) != string(plaintext) {
				t.Fatalf("round trip mismatch: got %q", decrypted)
			}
		})
	}
}

func TestStreamRoundTripAndCancellationCleanup(t *testing.T) {
	dir := t.TempDir()
	input := filepath.Join(dir, "input.txt")
	encrypted := filepath.Join(dir, "input.age")
	decrypted := filepath.Join(dir, "output.txt")
	if err := os.WriteFile(input, []byte(strings.Repeat("streaming payload\n", 1024)), 0600); err != nil {
		t.Fatal(err)
	}

	pair, err := GenerateKeyPair("")
	if err != nil {
		t.Fatal(err)
	}
	if err := EncryptStreamToFileWithRecipient(input, encrypted, pair.PublicKey, NewCancelToken()); err != nil {
		t.Fatal(err)
	}
	if err := DecryptStreamToFileWithIdentity(encrypted, decrypted, pair.PrivateKey, NewCancelToken()); err != nil {
		t.Fatal(err)
	}
	contents, err := os.ReadFile(decrypted)
	if err != nil {
		t.Fatal(err)
	}
	if !strings.HasPrefix(string(contents), "streaming payload") {
		t.Fatal("unexpected decrypted content")
	}

	cancelledOutput := filepath.Join(dir, "cancelled.age")
	token := NewCancelToken()
	token.Cancel()
	err = EncryptStreamToFileWithRecipient(input, cancelledOutput, pair.PublicKey, token)
	if !errors.Is(err, errCancelled) {
		t.Fatalf("error = %v, want cancellation", err)
	}
	if _, statErr := os.Stat(cancelledOutput); !errors.Is(statErr, os.ErrNotExist) {
		t.Fatalf("cancelled output still exists: %v", statErr)
	}
}

func TestTarCancellationAndInputValidation(t *testing.T) {
	dir := t.TempDir()
	input := filepath.Join(dir, "input.txt")
	if err := os.WriteFile(input, []byte("payload"), 0600); err != nil {
		t.Fatal(err)
	}

	mismatchOutput := filepath.Join(dir, "mismatch.tar")
	if err := TarFiles([]string{input}, nil, mismatchOutput, NewCancelToken()); err == nil {
		t.Fatal("expected mismatched input error")
	}
	if _, err := os.Stat(mismatchOutput); !errors.Is(err, os.ErrNotExist) {
		t.Fatalf("mismatch output exists: %v", err)
	}

	cancelledOutput := filepath.Join(dir, "cancelled.tar.gz")
	token := NewCancelToken()
	token.Cancel()
	err := TarGzipFiles([]string{input}, []string{"input.txt"}, cancelledOutput, token)
	if !errors.Is(err, errCancelled) {
		t.Fatalf("error = %v, want cancellation", err)
	}
	if _, statErr := os.Stat(cancelledOutput); !errors.Is(statErr, os.ErrNotExist) {
		t.Fatalf("cancelled archive still exists: %v", statErr)
	}
}
