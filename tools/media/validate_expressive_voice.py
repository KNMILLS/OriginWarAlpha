"""Check current expressive masters, exact inputs and reproducible radio exports.
ASR and levels are evidence with limits; they do not measure acting or listening.
"""
import hashlib,json,subprocess,wave
from pathlib import Path
import numpy as np
from compose_helmet_audio import ROOT,OUT,CACHE,export_ogg,sha
from render_alice_voice import word_tokens
from export_expressive_alice import decode_native,clean_voice,transmitted_voice,static_layer,TONE,TRANSMISSION

SOURCE=OUT/'voice/source/expressive'

def validate(source,index,authoritative,inspect,loudness):
    assert source['engine']=='Qwen3-TTS-VoiceDesign' and source['version']==1
    plan=json.loads((SOURCE/'delivery.json').read_text(encoding='utf-8'))
    assert sha(SOURCE/'delivery.json')==source['delivery_plan_sha256']
    assert len(source['clips'])==len(plan['clips'])==len(index['clips'])==9
    specs={e['key']:e for e in source['clips']};directions={e['key']:e for e in plan['clips']}
    assert set(specs)==set(directions)=={e['key'] for e in authoritative.values()}
    assert source['tone']==TONE and source['transmission']==TRANSMISSION and source['owner_direction']==plan['owner_direction']
    toolchain=json.loads((SOURCE/'toolchain.json').read_text(encoding='utf-8'))
    assert sha(SOURCE/'toolchain.json')==source['toolchain_sha256']
    assert toolchain['model']['revision']=='5ecdb67327fd37bb2e042aab12ff7391903235d3'
    for path,entry in toolchain['license_evidence'].items():assert sha(ROOT/path)==entry['sha256'],path
    result={}
    for i,runtime in enumerate(index['clips']):
        current=authoritative[(runtime['dialogue_id'],runtime['text_sha256'])];key=current['key']
        spec=specs[key];direction=directions[key]
        assert spec['full_text']==current['full_text'] and spec['spoken_spans']==current['spoken_spans']
        assert spec['full_text_sha256']==runtime['text_sha256']
        assert spec['synthesis_text']==' '.join(' '.join(current['spoken_spans']).split())
        assert hashlib.sha256(spec['synthesis_text'].encode()).hexdigest()==spec['synthesis_text_sha256']
        assert word_tokens(spec['synthesis_text'])==word_tokens(' '.join(current['spoken_spans']))
        assert spec['instruct']==direction['instruct'] and spec['seed']==direction['seed']
        assert spec['generator_source_sha256']==hashlib.sha256((ROOT/'tools/media/synthesize_expressive_alice.py').read_text(encoding='utf-8').encode()).hexdigest()
        path=SOURCE/spec['source_wav'];assert path.parent==SOURCE and sha(path)==spec['source_wav_sha256']
        with wave.open(str(path),'rb') as w:
            assert w.getframerate()==24000 and w.getnchannels()==1 and w.getsampwidth()==3
            assert w.getnframes()==round(spec['seconds']*24000)
        native=decode_native(path);assert np.isfinite(native).all() and len(native)>0 and np.abs(native).max()<1
        export=ROOT/'assets'/runtime['path'];assert sha(export)==runtime['sha256']
        rebuilt=transmitted_voice(path)
        second=CACHE/(key+'-expressive-reprocess.ogg');export_ogg(second,rebuilt,'ffmpeg')
        assert sha(second)==sha(export),'Transmitted radio reprocess differs: '+key
        second.unlink()
        clean=next(c for c in source['clean_exports'] if c['key']==key)
        clean_path=SOURCE/clean['path'];assert clean_path.parent==SOURCE/'clean' and sha(clean_path)==clean['sha256']
        second=CACHE/(key+'-clean-preservation.ogg');export_ogg(second,clean_voice(path),'ffmpeg')
        assert sha(second)==sha(clean_path),'Retained clean voice differs: '+key;second.unlink()
        assert sha(SOURCE/'word-screen.json')==source['word_screen_sha256']
        screen=json.loads((SOURCE/'word-screen.json').read_text(encoding='utf-8'))
        evidence=next(r for r in screen['records'] if r['file']==export.name)
        assert evidence['sha256']==sha(clean_path)
        transmitted=json.loads((SOURCE/'transmission-word-screen.json').read_text(encoding='utf-8'))
        assert sha(SOURCE/'transmission-word-screen.json')==source['transmission_word_screen_sha256']
        current_screen=next(r for r in transmitted['records'] if r['file']==export.name)
        assert current_screen['sha256']==sha(export)
        result[key]={**inspect(export,runtime['duration_seconds'],1),**loudness(export),
          'text_sha256':runtime['text_sha256'],'actual_synthesis_text_sha256':spec['synthesis_text_sha256'],
          'source_wav_sha256':sha(path),'source_rate':24000,'exact_input_words_and_campaign_binding':True,
          'source_reprocess_export_identical':True,'retained_clean_reprocess_identical':True,'ASR_screen_transcript':current_screen['text'],'historical_clean_ASR_transcript':evidence['text'],
          'ASR_scope':'Unprompted full-audio screening; recognition can differ. Exact input and successful reprocessing are separate from audible word/acting/identity acceptance.'}
    for path,expected in source['protected_nonvoice'].items():assert sha(ROOT/path)==expected,path
    return result

def validate_static(source,inspect):
    entry=source['radio_static'];path=ROOT/'assets'/entry['path']
    assert entry['speech_free_by_construction'] and sha(path)==entry['sha256']
    second=CACHE/'radio-static-expressive-reprocess.ogg';export_ogg(second,static_layer(),'ffmpeg')
    assert sha(second)==sha(path),'Separate static loop differs';second.unlink()
    screen=json.loads((SOURCE/'transmission-word-screen.json').read_text(encoding='utf-8'))
    record=next(r for r in screen['records'] if r['file']=='radio-static.ogg')
    assert record['sha256']==sha(path) and not record['text'].strip()
    return {**inspect(path,12,1,True),'independent_speech_free_texture':True,'source_reprocess_export_identical':True}
