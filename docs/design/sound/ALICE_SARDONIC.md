# VOICE2: A.L.I.C.E.'s dry delivery

Base: `039bebc8e0d36ab2497bd648c46da6de67d0fe2b`. Owner feedback: **“ALICE needs to sound more sarcastic.”** This pass changes actual speech performance, with no new dialogue, story, Java, catalog schema, music, effects or suit layers. Sarcasm is the production target; hearing and owner acceptance remain separate gates.

The voice should suggest that she has already noticed the absurdity and does not need to announce the joke. Brief inventory-like setups give way to delayed, quieter barbs. Personhood questions remain genuine. Later anxiety sits beneath controlled resolve rather than turning her final choice into a comedy routine.

| Scene | Authored performance choice |
| --- | --- |
| First meeting | Matter-of-fact list, pointed “incomplete information,” a beat before the public-service observation; firm refusal, then a dry invitation to share inconvenience. |
| Canceled project | Official acronym expansion; a short “Yes”; a separate, falling acronym comment. Plain “It worked,” then a held beat before the difficulty reversal. |
| Agency | A genuine question about ownership; crisp distinction between knowing a definition and asking about a person. |
| Refusal | Controlled anxiety, firm remaining choice; a small release before the public-facts barb. |
| Sacrifice / hatch | Bitter understatement rather than a gag; sincere plea and resolve. Later anxiety remains more present than in the early exchanges. |
| Companion variants | Quiet corrections about telemetry, manufacturers, doors, orders and oxygen. The practical information stays clear. |

There are **48 authored phrase units across the same nine clips**. Their editable text, pacing, pauses, phrase levels, tail contours and intent are in assets/audio/voice/source/sardonic-delivery.json. Ordered word tokens match the authoritative Campaign quoted spans exactly; punctuation and capitalization change only for synthesis. Full Campaign text hashes, clip paths, index version and runtime field names remain unchanged.

Each phrase is genuinely synthesized with the existing permitted Piper model. Actual pace settings range from.96 to1.10. Explicit beat pauses are at most.50s. Most phrase endings fall gently, up to.85 semitone, only over their final portion; one genuine question has a slight rising end. Phrase-level emphasis is authored through grouping, pacing and restrained gain. No guessed word alignment or global pitch shift is used. Numeric silence is trimmed with40ms protection around speech; “clipped” means a concise release, not removed consonants or words.

Early anxiety controls are.02–.12; refusal/sacrifice rise to.42/.50. The dry radio band and low static remain, with less early tremor. The pre-encode speech ceiling is−10dBFS to reserve mix headroom. This is a controlled synthetic performance, not an actor imitation, trained emotional model, or claim that a waveform metric measures sarcasm.

## Source, A/B and reproduction

- Runtime: the same nine assets/audio/voice/*.ogg, mono44.1kHz, with unchanged audio/voice/index.json schema.
- Source: retained phrase WAVs and assembled dry sources are PCM16 mono22,050Hz. dialogue-source.json binds their hashes, exact text and delivery-plan hash. Reprocessing retained sources does not require the large model/tool cache.
- Prior complete voice set: recoverable at the exact base commit. voice2-baseline.json retains its catalog, source metadata hash and47 protected nonvoice hashes. Existing permissions/model/tool pins and the earlier first-attempt evidence are preserved.
- Compact comparison: assets/audio/voice/source/comparison/alice-record-before-after.ogg. First9.414785s are the prior clip, then.8s silence, then the9.223401s candidate; total19.438186s. Separate before/after OGGs and a hash/timing receipt sit beside it. These authoring files are outside the runtime glob.

Run `python tools/media/render_alice_voice.py --repeatability` for independent phrase synthesis repeats, or `--reprocess-only` for pinned dry-source export. `--only alice-record` supports a bounded authoring comparison. Then run `python tools/media/validate_audio.py`. The selected general female voice, public-domain-data/model evidence and original MIT Piper tool are unchanged from ATM-SOUND; no new service, model, sample or franchise recording was introduced.

## Verification and remaining review

All48 phrase inputs were independently synthesized twice with identical WAV bytes; phrase assembly also repeated identically. Validation checks exact TTS-input word order, all nine Campaign bindings, source phrase/header/hash/assembly identity, final source-to-OGG repeatability, full decoding/durations/channels/levels, catalog path/schema continuity,47 unchanged nonvoice files, and bounded summed mix headroom. The current receipt is assets/audio/validation.json. Java/core/story files are unchanged, so this lease adds no gameplay test claim.

Root owns the actual held ALICE scene, campaign/package checks and listening. Review the A/B for dryness, timing, intelligibility and whether the reversal lands; then check meeting, agency and the late refusal/sacrifice to ensure the same character persists. The absence of clipping or a source-hash mismatch does not establish those dramatic results. No subjective listening, native playback or owner approval is claimed here.
