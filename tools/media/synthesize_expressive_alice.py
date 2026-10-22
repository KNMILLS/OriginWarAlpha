"""Author native ALICE performances offline; model/tool bytes are never game dependencies.

Text and acting instructions are separate. Outputs are candidates until independently
screened; hashes and exact inputs do not establish the generated words or heard tone.
"""
import os
os.environ.setdefault('HF_HUB_OFFLINE','1')
os.environ.setdefault('TOKENIZERS_PARALLELISM','false')
import argparse,hashlib,importlib.metadata,json,random,time
from pathlib import Path
import numpy as np
import soundfile as sf
import torch
from qwen_tts import Qwen3TTSModel

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    p=argparse.ArgumentParser();p.add_argument('--plan',type=Path,required=True)
    p.add_argument('--model-dir',type=Path,required=True);p.add_argument('--output-dir',type=Path,required=True)
    p.add_argument('--keys',nargs='+');args=p.parse_args()
    plan=json.loads(args.plan.read_text(encoding='utf-8-sig'))
    root=Path(__file__).resolve().parents[2]
    campaign=root/'core/src/main/java/io/zipcoder/originwar/campaign/Campaign.java'
    campaign_sha=hashlib.sha256(campaign.read_text(encoding='utf-8').encode()).hexdigest()
    assert campaign_sha==plan['campaign_source']['canonical_utf8_lf_sha256']
    clips={e['key']:e for e in plan['clips']};keys=args.keys or list(clips)
    assert len(set(keys))==len(keys) and all(k in clips for k in keys)
    args.output_dir.mkdir(parents=True,exist_ok=True)
    for key in keys:assert not (args.output_dir/(key+'-dry.wav')).exists(),key
    torch.set_num_threads(8)
    model=Qwen3TTSModel.from_pretrained(str(args.model_dir),device_map='cuda:0',dtype=torch.bfloat16,attn_implementation='sdpa',local_files_only=True)
    print('MODEL READY',flush=True)
    for key in keys:
        entry=clips[key];text=entry['synthesis_text'];instruction=entry['instruct'];seed=entry['seed']
        assert hashlib.sha256(text.encode()).hexdigest()==entry['synthesis_text_sha256']
        assert ' '.join(' '.join(entry['spoken_spans']).split())==text
        random.seed(seed);np.random.seed(seed);torch.manual_seed(seed);torch.cuda.manual_seed_all(seed)
        kwargs={'non_streaming_mode':True,'max_new_tokens':768,'do_sample':True,'temperature':.9,'top_p':1.,'top_k':50,'repetition_penalty':1.05,'subtalker_dosample':True,'subtalker_temperature':.9,'subtalker_top_k':50,'subtalker_top_p':1.}
        print('START '+key,flush=True);start=time.monotonic()
        wavs,rate=model.generate_voice_design(text=text,language='English',instruct=instruction,**kwargs)
        x=np.asarray(wavs[0]);path=args.output_dir/(key+'-dry.wav')
        finite=bool(np.isfinite(x).all());seconds=len(x)/rate
        if not finite:np.save(args.output_dir/(key+'-invalid-native.npy'),x)
        sf.write(path,x,rate,subtype='PCM_24')
        record={**entry,'engine':'Qwen3-TTS-VoiceDesign','model_revision':plan['model']['revision'],
          'campaign_source_sha256':campaign_sha,'source_wav':path.name,'source_wav_sha256':sha(path),
          'source_rate':rate,'source_subtype':'PCM_24','seconds':seconds,'finite':finite,
          'peak_linear':float(np.abs(x).max()),'generation':kwargs,'language':'English',
          'elapsed_seconds':time.monotonic()-start,'torch':torch.__version__,'qwen_tts':importlib.metadata.version('qwen-tts'),
          'generator_source_sha256':hashlib.sha256(Path(__file__).read_text(encoding='utf-8').encode()).hexdigest(),
          'quality_scope':'Native generated candidate; input binding/hash/header checks do not establish output words, speaker identity or heard tone.'}
        (args.output_dir/(key+'.json')).write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8',newline='\n')
        print(json.dumps({k:record[k] for k in ('key','seconds','finite','peak_linear','elapsed_seconds','source_wav_sha256')}),flush=True)
        assert rate==24000 and x.ndim==1 and finite and 2<seconds<55 and record['peak_linear']<1,'Reject invalid/unbounded native output; preserve candidate.'

if __name__=='__main__':main()
