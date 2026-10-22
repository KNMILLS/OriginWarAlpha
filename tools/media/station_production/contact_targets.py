"""Project authored physical contact points from the exact fixed sprite camera."""
from pathlib import Path
import json,math,hashlib

ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'assets/art/production/station'
angle=math.atan2(4.59,6);k=2/2.65

def projected(point):
    x,y,z=point
    return [x*k,k*(y*math.sin(angle)+z*math.cos(angle))]

def rotated_grip(point):
    x,y,z=point;y+=.20;z-=.42;a=math.radians(115)
    return [x,-.20+y*math.cos(a)-z*math.sin(a),.42+y*math.sin(a)+z*math.cos(a)]

targets=[]
for name,point,description in [
    ('terminal.keyboard-key-top',[-.026,-.203,.910],'Top of actual central-right physical key, not the console origin'),
    ('terminal.keyboard-row-center',[-.08,-.203,.910],'Keyboard middle row top; includes the authored left offset'),
    ('terminal.release-button-top',[.29,-.20,.930],'Top of physical amber release button; no powered-light implication'),
    ('switch.closed-grip-center',[0,-.30,.66],'Center of physical insulated lever grip'),
    ('switch.closed-grip-front',[0,-.335,.66],'Front contact face of physical insulated lever grip'),
    ('switch.released-grip-center',rotated_grip([0,-.30,.66]),'Physical grip after115degree source throw around[0,-.20,.42]'),
]:
    targets.append(dict(id=name,worldPoint=point,screenCells=projected(point),description=description,propFacing='south',requiredFrontApproachOfficerFacing='north'))

data=dict(scope='Physical source projection only. Current prop renders expose their south interface; other cardinal object orientations are not authored here. Root must animate actual reach/advance without moving collision or faking an interface.',basis=dict(cameraDelta=[0,-6,4.59],orthoScale=2.65,logicalCanvasHeight=2,pivotOrigin='bottom-left',xPositive='right',yPositive='up',formula='screenCells=[2*x/2.65,2*(y*sin(atan2(4.59,6))+z*cos(atan2(4.59,6)))/2.65]'),sourceScriptSha256=hashlib.sha256((Path(__file__).parent/'build.py').read_bytes()).hexdigest(),contacts=targets)
(OUT/'source/CONTACT_TARGETS.json').write_text(json.dumps(data,indent=2)+'\n')
print(json.dumps(data))
