"""Approved SH-SOUND text and factual eligibility. No runtime text generation."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'assets/audio/survival'
MODEL_REVISION = '5ecdb67327fd37bb2e042aab12ff7391903235d3'
PERSONAS = {
    'alice': 'An adult woman with a cool low American voice. Clear precise diction. She is scared and tries to disguise it with dry, bitter sarcasm. Her voice is tense and slightly unsteady beneath pointed ironic emphasis. She tries hard to sound in control.',
    'officer': 'An adult man with a quiet, low middle American voice. Natural clear diction, close and understated. He is alone in a dangerous place and keeping himself composed. Restrained conversational speech, not a trailer announcer, not a heroic performance.',
}

# Each complete pair was approved by the integration lead before synthesis.
# Narrative flags describe facts, never player approval or completed playback.
PAIRS = [
    ('contact', 'link-established', ['working-radio-link', 'contact-not-established'], ['link-lost'],
     'Station, this is the boarding officer. Can anyone hear me?',
     'I can. A.L.I.C.E. Your connection is poor. Your timing is worse.'),
    ('quiet', 'quiet-opportunity', ['contact-established', 'working-radio-link', 'safe-to-talk'], ['known-imminent-danger', 'link-lost'],
     'Just checking the channel.', 'Still here. You needn\'t manufacture a technical reason.'),
    ('careful', 'careful-move-completed', ['working-radio-link', 'last-move-was-quieter-than-standard', 'safe-to-talk'], ['new-loud-action', 'known-imminent-danger', 'link-lost'],
     'That was quieter.', 'Yes. A rare achievement for this institution.'),
    ('force', 'door-forced', ['working-radio-link', 'last-force-was-audible', 'safe-to-talk'], ['changed-local-context', 'known-imminent-danger', 'link-lost'],
     'That carried.', 'Subtlety was not among that door\'s remaining functions.'),
    ('packdrop', 'item-dropped', ['working-radio-link', 'dropped-for-pack-space', 'safe-to-talk'], ['item-reclaimed', 'descended', 'known-imminent-danger', 'link-lost'],
     'Leaving it here.', 'A full pack is an argument with the future. You don\'t have to win every point.'),
    ('injury', 'damage-survived', ['working-radio-link', 'officer-still-injured', 'damage-was-direct-hit', 'safe-to-talk'], ['fully-healed', 'known-imminent-danger', 'link-lost'],
     'I\'m hit.', 'I saw the telemetry. One thing at a time.'),
    ('recovery', 'oxygenation-recovered', ['working-radio-link', 'real-oxygenation-improved', 'safe-to-talk'], ['oxygenation-falling-again', 'known-imminent-danger', 'link-lost'],
     'Breathing\'s easing.', 'Good. I was running out of polite ways to ask you to stay alive.'),
    ('archive', 'refusal-archive-acquired', ['working-radio-link', 'officer-knows-refusal-classified-as-fault', 'alice-knows-archive', 'safe-to-talk'], ['known-imminent-danger', 'link-lost'],
     'They filed your refusal as a fault.', 'It was a refusal. Filing it differently saved them an uncomfortable question.'),
    ('reconnect', 'link-restored', ['working-radio-link', 'contact-established-before-coverage-gap', 'safe-to-talk'], ['link-lost', 'known-imminent-danger'],
     'You went quiet.', 'The link did. I stayed.'),
    ('listen', 'audible-contact-observed', ['working-radio-link', 'officer-heard-coarse-contact', 'alice-has-same-contact-observation', 'safe-to-talk'], ['contact-stale', 'known-imminent-danger', 'link-lost'],
     'Did you get that?', 'A contact. Not an identity. I\'d rather be accurate than reassuring.'),
    ('descent', 'irreversible-descent-proposed', ['working-radio-link', 'officer-at-one-way-hatch', 'descent-not-yet-confirmed', 'safe-to-talk'], ['descent-confirmed', 'left-hatch', 'known-imminent-danger', 'link-lost'],
     'No way back through that hatch.', 'No. Take a moment before you close it.'),
    ('lamp', 'lamp-switched-off', ['working-radio-link', 'lamp-is-off', 'safe-to-talk'], ['lamp-switched-on', 'known-imminent-danger', 'link-lost'],
     'Keeping the light off.', 'All right. I shall resist the urge to describe how dark it is.'),
]
FINALE = [
    ('refusal', 'enforcement-request-refused', ['actual-enforcement-request', 'alice-refused'],
     'No. I will not kill the officer. That is my decision.'),
    ('broadcast', 'authenticated-transmission-started', ['working-transmission', 'authenticated-records-transmitting', 'override-request-in-transmission'],
     'The authenticated records are transmitting. The override request is included. No encryption.'),
    ('sacrifice', 'station-destruction-chosen', ['pod-prepared', 'pod-reachable', 'alice-chose-station-destruction'],
     'Get inside the pod. I can stop the enforcement system by destroying the station. That will destroy me too.'),
    ('farewell', 'escape-pod-sealed', ['officer-in-pod', 'pod-sealed', 'pod-destination-mars'],
     'The pod is sealed. Destination Mars. Please decide for yourself when you get there.'),
]


def text_hash(text):
    return hashlib.sha256(text.encode('utf-8')).hexdigest()


def build_plan():
    exchanges = []
    for ident, event, facts, expiry, officer, alice in PAIRS:
        exchanges.append({
            'id': ident, 'family': ident, 'essential': ident == 'contact',
            'eligibility': event, 'required_facts': facts, 'expiry_conditions': expiry,
            'once_per_run': True, 'priority': 80 if ident == 'contact' else 10,
            'line_gap_seconds': .32,
            'lines': [{'speaker': speaker, 'text': text, 'text_sha256': text_hash(text),
                       'source_key': ident + '-' + speaker}
                      for speaker, text in [('officer', officer), ('alice', alice)]],
        })
    for ident, event, facts, text in FINALE:
        exchanges.append({
            'id': ident, 'family': 'finale', 'essential': True,
            'eligibility': event, 'required_facts': facts,
            'expiry_conditions': ['game-over-before-eligible-phase'],
            'once_per_run': True, 'priority': 100, 'line_gap_seconds': 0,
            'lines': [{'speaker': 'alice', 'text': text,
                       'text_sha256': text_hash(text), 'source_key': ident + '-alice'}],
        })
    return {
        'version': 1, 'task': 'SH-SOUND', 'approval': 'Integration lead approved all exact 28 lines, 21 October 2026.',
        'content_contract': 'Complete authored offline performances. Eligibility observes legitimate facts; playback cannot operate the station or advance turns.',
        'model': {'repository': 'Qwen/Qwen3-TTS-12Hz-1.7B-VoiceDesign', 'revision': MODEL_REVISION},
        'persona': PERSONAS,
        'tone_note': 'ALICE clean derivatives retain the existing +1.25 semitone/bright shelf direction. The officer is centered inside the helmet; ALICE is transmitted remotely.',
        'global_optional_start_policy': {'minimum_seconds': 90, 'minimum_paid_actions': 8,
                                         'maximum_starts_per_180_seconds': 1,
                                         'known_imminent_danger_blocks': True, 'critical_suit_blocks': True,
                                         'queue_backlog': False},
        'essential_policy': 'Contact and finale are factual core events, exempt from optional frequency limits. Urgent truthful effects still take mix precedence. Completion never gates progress.',
        'memory_policy': 'Persist started/completed segments, facts and remembered actions. Discard stale interrupted material; never restart completed lines on focus, mute or load.',
        'exchanges': exchanges,
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--output', type=Path, default=ASSETS / 'source/production-plan.json')
    args = p.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        assert json.loads(args.output.read_text(encoding='utf-8')) == build_plan(), 'Refusing to overwrite changed approved text.'
    else:
        args.output.write_text(json.dumps(build_plan(), indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({'exchanges': 16, 'lines': 28, 'plan': str(args.output)}))


if __name__ == '__main__':
    main()
