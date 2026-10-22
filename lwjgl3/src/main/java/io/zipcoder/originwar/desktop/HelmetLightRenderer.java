package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import io.zipcoder.originwar.model.GameState;

/** Soft pixel-space helmet beam over SquidGrid's shadowed fixture field. UI uses the normal shader. */
final class HelmetLightRenderer implements AutoCloseable {
    private final StationLighting lighting=new StationLighting();
    private Pixmap pixels;
    private Texture texture;
    private final ShaderProgram shader;
    HelmetLightRenderer(){
        shader=new ShaderProgram("""
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying vec2 v_world;
            void main(){v_color=a_color;v_color.a*=255.0/254.0;v_texCoords=a_texCoord0;v_world=a_position.xy;gl_Position=u_projTrans*a_position;}
            ""","""
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying vec2 v_world;
            uniform sampler2D u_texture;
            uniform sampler2D u_lighting;
            uniform vec2 u_gridSize;
            uniform vec2 u_origin;
            uniform vec2 u_lamp;
            uniform vec2 u_direction;
            uniform vec2 u_screen;
            uniform float u_tile;
            uniform float u_enabled;
            uniform float u_minimum;
            void main(){
                vec2 grid=vec2((v_world.x-u_origin.x)/u_tile,(u_origin.y+u_tile-v_world.y)/u_tile);
                vec2 uv=grid/u_gridSize;
                vec4 field=texture2D(u_lighting,uv);
                float visible=step(.99,texture2D(u_lighting,(floor(grid)+.5)/u_gridSize).a);
                vec2 delta=(v_world-u_lamp)/u_tile;
                float distance=length(delta);
                float angle=dot(delta/max(.001,distance),u_direction);
                float cone=smoothstep(.74,.92,angle)*(1.0-smoothstep(1.2,8.5,distance))*.95;
                float local=(1.0-smoothstep(.3,2.1,distance))*.72;
                vec3 light=field.rgb+(cone+local)*u_enabled*visible*vec3(.91,.95,1.0);
                light=max(light,vec3(u_minimum)*visible);
                vec2 edge=(v_world/u_screen-.5)*vec2(1.22,1.0);
                float visor=1.0-.42*smoothstep(.38,.76,length(edge));
                vec4 material=texture2D(u_texture,v_texCoords)*v_color;
                gl_FragColor=vec4(material.rgb*light*visor,material.a);
            }
            """);
        if(!shader.isCompiled())throw new IllegalStateException("Helmet lighting shader: "+shader.getLog());
    }
    StationLighting field(){return lighting;}
    void begin(SpriteBatch batch,GameState s,float originX,float originY,float tile,float lampX,float lampY,float direction,boolean enabled,boolean reduced,java.util.List<StationLighting.Pulse> pulses){
        lighting.update(s,reduced);
        for(var pulse:pulses)lighting.pulse(s,pulse);
        if(pixels==null||pixels.getWidth()!=s.width||pixels.getHeight()!=s.height){
            if(pixels!=null)pixels.dispose();if(texture!=null)texture.dispose();
            pixels=new Pixmap(s.width,s.height,Pixmap.Format.RGBA8888);texture=new Texture(s.width,s.height,Pixmap.Format.RGBA8888);
            texture.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        }
        for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)pixels.drawPixel(x,y,lighting.rgba(s,x,y));
        batch.flush();texture.draw(pixels,0,0);texture.bind(1);Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        batch.setShader(shader);shader.setUniformi("u_lighting",1);
        shader.setUniformf("u_gridSize",s.width,s.height);shader.setUniformf("u_origin",originX,originY);
        shader.setUniformf("u_lamp",lampX,lampY);shader.setUniformf("u_direction",(float)Math.cos(direction),(float)Math.sin(direction));
        shader.setUniformf("u_screen",Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
        shader.setUniformf("u_tile",tile);shader.setUniformf("u_enabled",enabled?1:0);shader.setUniformf("u_minimum",0);
    }
    void actorFloor(SpriteBatch batch){batch.flush();shader.setUniformf("u_minimum",.28f);}
    void end(SpriteBatch batch){batch.setShader(null);batch.setColor(Color.WHITE);}
    @Override public void close(){shader.dispose();if(texture!=null)texture.dispose();if(pixels!=null)pixels.dispose();}
}
