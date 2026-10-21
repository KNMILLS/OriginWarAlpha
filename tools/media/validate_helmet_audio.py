"""Independent full export decoding, binding, rights hashes and mix-headroom audit.
Numbers are diagnostics, not audible/expressive or backend acceptance.
"""
from pathlib import Path
import hashlib,json,math,re,subprocess,wave
import numpy as np
from compose_helmet_audio import ROOT,OUT,CACHE,SR,write_wav,export_ogg,sha
from render_alice_voice import extract,radio,PLAN,delivery,assemble

def db(x):return round(20*math.log10(max(1e-12,float(x))),3)
def decode(path):
    data=subprocess.run(['ffmpeg','-nostdin','-v','error','-i',str(path),'-f','f32le','-acodec','pcm_f32le','-'],capture_output=True,check=True).stdout
    probe=json.loads(subprocess.run(['ffprobe','-v','error','-show_entries','stream=codec_name,sample_rate,channels','-of','json',str(path)],capture_output=True,text=True,check=True).stdout)['streams'][0]
    assert int(probe['sample_rate'])==SR
    x=np.frombuffer(data,'<f4').reshape(-1,probe['channels'])
    assert len(x)>0 and np.isfinite(x).all() and np.max(np.abs(x))<.99
    return x,probe

def loudness(path):
    p=subprocess.run(['ffmpeg','-nostdin','-hide_banner','-i',str(path),'-af','loudnorm=I=-23:TP=-2:LRA=11:print_format=json','-f','null','-'],capture_output=True,text=True,check=True)
    block=re.findall(r'\{\s*"input_i".*?\}',p.stderr,re.S)[-1];x=json.loads(block)
    return {'integrated_lufs':float(x['input_i']),'true_peak_dbtp':float(x['input_tp']),'loudness_range_lu':float(x['input_lra'])}

def inspect(path,seconds,channels,loop=False):
    x,p=decode(path);assert x.shape[1]==channels
    assert abs(len(x)/SR-seconds)<.0001,(path,len(x)/SR,seconds)
    result={'sha256':sha(path),'bytes':path.stat().st_size,'duration_seconds':len(x)/SR,
      'channels':channels,'sample_rate':SR,'decoded_peak_dbfs':db(np.max(np.abs(x))),
      'decoded_rms_dbfs':db(np.sqrt(np.mean(x*x))),'peak_linear':float(np.max(np.abs(x)))}
    if loop:
        delta=float(np.max(np.abs(x[0]-x[-1])));p99=float(np.quantile(np.abs(np.diff(x,axis=0)),.99))
        first,last=(db(np.sqrt(np.mean(y*y))) for y in (x[:882],x[-882:]))
        flagged=(delta>.002 and delta/max(p99,1e-12)>3) or (max(first,last)>-60 and abs(first-last)>8)
        result['loop_metrics']={'seam_sample_delta':delta,'adjacent_step_p99':p99,
          'first_20ms_rms_dbfs':first,'last_20ms_rms_dbfs':last,'focused_listening_flag':flagged,
          'interpretation':'File-boundary diagnostic only; real backend looping and audible seam review remain open.'}
    return result

def main():
    m=json.loads((OUT/'manifest.json').read_text());index=json.loads((OUT/'voice/index.json').read_text())
    expressive=OUT/'voice/source/expressive/manifest.json'
    source=json.loads((expressive if expressive.exists() else OUT/'voice/source/dialogue-source.json').read_text(encoding='utf-8'))
    runtime=json.loads((OUT/'runtime.json').read_text())
    assert len(m['music'])==6 and len(m['sfx'])==34 and len(m['ambience'])==6 and len(index['clips'])==9
    assert runtime['sfx']=={k:v['duration_seconds'] for k,v in m['sfx'].items()}
    campaign=ROOT/'core/src/main/java/io/zipcoder/originwar/campaign/Campaign.java'
    assert source['campaign_source_sha256']==hashlib.sha256(campaign.read_text(encoding='utf-8').encode('utf-8')).hexdigest()
    authoritative={(x['dialogue_id'],x['text_sha256']):x for x in extract()}
    delivery_plan=json.loads(PLAN.read_text(encoding='utf-8'))
    report={'scope':'Full decode, headers, levels, loop-boundary metrics, exact Campaign text spans, deterministic source reprocessing, source/license hashes and conservative full-slider mix headroom. No audible/runtime/owner acceptance.',
      'music':{},'sfx':{},'ambience':{},'voice':{},'license_evidence':{},'repeatability':m['repeatability']}
    for role,entry in m['music'].items():
        assert entry['duration_seconds']>=(90 if role in ('exploration','combat','alice') else 60)
        path=ROOT/entry['path'];assert sha(path)==entry['sha256']
        report['music'][role]={**inspect(path,entry['duration_seconds'],2,True),**loudness(path)}
    for group in ('sfx','ambience'):
        for name,entry in m[group].items():
            path=ROOT/entry['path'];assert sha(path)==entry['sha256']
            if group=='sfx':
                with wave.open(str(path),'rb') as w:assert w.getnchannels()==1 and w.getsampwidth()==2 and w.getframerate()==SR
            report[group][name]=inspect(path,entry['duration_seconds'],1,group=='ambience')
    if expressive.exists():
        from validate_expressive_voice import validate,validate_static
        report['voice']=validate(source,index,authoritative,inspect,loudness)
        report['radio_static']=validate_static(source,inspect)
    else:
        for i,entry in enumerate(index['clips']):
            match=authoritative[(entry['dialogue_id'],entry['text_sha256'])]
            authored=source['clips'][i];assert match['full_text']==authored['full_text'] and match['spoken_spans']==authored['spoken_spans']
            preset=delivery(match,delivery_plan)
            assert authored['delivery_plan_sha256']==sha(PLAN)
            assert len(authored['phrases'])==len(preset['phrases'])
            for record,phrase in zip(authored['phrases'],preset['phrases']):
                assert record['synthesis_text']==phrase['text']
                assert sha(OUT/'voice/source'/record['file'])==record['sha256']
            assert hashlib.sha256(authored['full_text'].encode()).hexdigest()==entry['text_sha256']
            rawpath=OUT/'voice/source'/(match['key']+'-dry.wav');assert sha(rawpath)==authored['source_wav_sha256']
            assert 'identical' in authored['repeatability']
            path=ROOT/'assets'/entry['path'];assert sha(path)==entry['sha256']
            with wave.open(str(rawpath),'rb') as w:
                rate=w.getframerate();raw_frames=w.readframes(w.getnframes())
                raw=np.frombuffer(raw_frames,'<i2').astype(np.float32)/32768
            rebuilt=assemble(authored['phrases'],preset)
            assert np.rint(np.clip(rebuilt,-1,1)*32767).astype('<i2').tobytes()==raw_frames,'Phrase assembly differs: '+match['key']
            repeat=radio(raw,rate,authored['fear'],2115+i)
            second=CACHE/(match['key']+'-reprocess.ogg');export_ogg(second,repeat,'ffmpeg')
            assert sha(second)==sha(path),'Reprocessed voice export differs: '+match['key'];second.unlink()
            report['voice'][match['key']]={**inspect(path,entry['duration_seconds'],1),**loudness(path),
                 'text_sha256':entry['text_sha256'],'source_wav_sha256':sha(rawpath),
                 'word_tokens_and_campaign_binding_exact':True,'delivery_phrase_count':len(preset['phrases']),
                 'delivery_plan_sha256':sha(PLAN),'source_phrase_assembly_identical':True,
                 'source_reprocess_export_identical':True}
    lock=json.loads((OUT/'voice/source/toolchain.json').read_text())
    for name,entry in lock['evidence'].items():
        path=OUT/'voice/source'/name;assert sha(path)==entry['sha256'];report['license_evidence'][name]=entry
    assert lock['voice_repository_revision']=='5041a7a49ae5b3a0f30e6837769eb03fdbb231bd'
    assert lock['downloads']['en_US-ljspeech-high.onnx']['sha256']=='5d4f08ba6a2a48c44592eed3ce56bf85e9de3dd4e20df90541ae68a8310c029a'
    # Worst-case sum of peak magnitudes (stricter than a representative mixed WAV).
    # Crossfades are convex pairs. Six one-shots share .65/sqrt(N), maximum gain .82.
    peak=lambda entries:max(e['peak_linear'] for e in entries.values())
    breath=max(e['peak_linear'] for k,e in report['ambience'].items() if k.startswith('breath'))
    heart=max(e['peak_linear'] for k,e in report['ambience'].items() if k.startswith('heartbeat'))
    bound=.70*(peak(report['music'])*.68+breath*.90+heart*.61+peak(report['voice'])*.86+peak(report['sfx'])*.82*.65*math.sqrt(6)+report.get('radio_static',{}).get('peak_linear',0)*.65*1.75)
    assert bound<1,('Full-slider conservative peak bound',bound)
    report['mix_headroom']={'full_slider_conservative_peak_bound':bound,'dbfs_upper_bound':db(bound),
      'assumptions':'Current director .70 global reserve, .68 score, .86 voice ceiling (reception only attenuates), .65 independent radio texture with conservative1.75 swell ceiling, .90/.61 maximum body gains, .65/sqrt(N) six-effect bus and .82 maximum effect gain. Crossfade weights sum to one. No audible matching implied.'}
    all_hashes=[e['sha256'] for group in ('music','sfx','ambience','voice') for e in report[group].values()]
    assert len(set(all_hashes))==len(all_hashes)
    report['source_hashes_format']='UTF-8 canonical LF, matching Git source archives across platform checkouts'
    names=('compose_audio.py','compose_helmet_audio.py','prepare_alice_voice.py','render_alice_voice.py','validate_helmet_audio.py')
    if expressive.exists():names+=('synthesize_expressive_alice.py','export_expressive_alice.py','validate_expressive_voice.py')
    report['source_hashes']={name:hashlib.sha256((ROOT/'tools/media'/name).read_text(encoding='utf-8').encode('utf-8')).hexdigest() for name in names}
    report['result']='PASS export/source/mix-bound validation; listening and actual runtime remain separate'
    baseline=json.loads((OUT/'voice/source/voice2-baseline.json').read_text(encoding='utf-8'))
    for path,expected in baseline['unchanged_nonvoice'].items():assert sha(ROOT/path)==expected,path
    before={(e['dialogue_id'],e['text_sha256']) for e in baseline['voice_index']['clips']}
    assert before=={(e['dialogue_id'],e['text_sha256']) for e in index['clips']}
    assert {(e['dialogue_id'],e['text_sha256']):e['path'] for e in baseline['voice_index']['clips']}=={(e['dialogue_id'],e['text_sha256']):e['path'] for e in index['clips']}
    assert set(baseline['voice_index'])==set(index) and all(set(e)==set(index['clips'][0]) for e in baseline['voice_index']['clips'])
    if expressive.exists():
        report['voice3']={'engine':source['engine'],'native_masters':len(source['clips']),'tone':source['tone'],'delivery_plan_sha256':source['delivery_plan_sha256'],'unchanged_nonvoice_files':len(source['protected_nonvoice']),'current_source':'voice/source/expressive/manifest.json','scope':'Current expressive synthesis; exact inputs/reprocessing/ASR are separate from human word, acting and identity acceptance.'}
        chain=json.loads((OUT/'voice/source/expressive/toolchain.json').read_text(encoding='utf-8'))
        report['license_evidence'].update(chain['license_evidence'])
    else:
        report['voice2']={'delivery_plan_sha256':sha(PLAN),'phrases':sum(len(v['phrases']) for v in delivery_plan['clips'].values()),
            'unchanged_nonvoice_files':len(baseline['unchanged_nonvoice']),'full_text_bindings_and_catalog_schema_unchanged':True,
            'prior_voice_recoverable_commit':baseline['base_commit']}
    (OUT/'validation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'result':report['result'],'counts':{g:len(report[g]) for g in ('music','sfx','ambience','voice')},
       'longest_voice_seconds':max(e['duration_seconds'] for e in report['voice'].values()),
       'voice_peak_max_dbfs':db(peak(report['voice'])),'music_lufs':{k:v['integrated_lufs'] for k,v in report['music'].items()},
       'loop_flags':[g+'/'+k for g in ('music','ambience') for k,v in report[g].items() if v['loop_metrics']['focused_listening_flag']],
       'mix_headroom':report['mix_headroom']},indent=2))

if __name__=='__main__':main()
