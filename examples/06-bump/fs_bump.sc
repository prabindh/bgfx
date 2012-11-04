$input v_wpos, v_view, v_normal, v_tangent, v_texcoord0

/*
 * Copyright 2011-2012 Branimir Karadzic. All rights reserved.
 * License: http://www.opensource.org/licenses/BSD-2-Clause
 */

#include "../common/common.sh"

SAMPLER2D(u_texColor, 0);
SAMPLER2D(u_texNormal, 1);
uniform vec4 u_lightPosRadius[4];
uniform vec4 u_lightRgbInnerR[4];

vec2 blinn(vec3 _lightDir, vec3 _normal, vec3 _viewDir)
{
	float ndotl = dot(_normal, _lightDir);
	vec3 reflected = _lightDir - 2.0*ndotl*_normal; // reflect(_lightDir, _normal);
	float rdotv = dot(reflected, _viewDir);
	return vec2(ndotl, rdotv);
}

float fresnel(float _ndotl, float _bias, float _pow)
{
	float facing = (1.0 - _ndotl);
	return max(_bias + (1.0 - _bias) * pow(facing, _pow), 0.0);
}

vec4 lit(float _ndotl, float _rdotv, float _m)
{
	float diff = max(0.0, _ndotl);
	float spec = step(0.0, _ndotl) * max(0.0, _rdotv * _m);
	return vec4(1.0, diff, spec, 1.0);
}

vec4 powRgba(vec4 _rgba, float _pow)
{
	vec4 result;
	result.xyz = pow(_rgba.xyz, vec3_splat(_pow) );
	result.w = _rgba.w;
	return result;
}

vec4 toLinear(vec4 _rgba)
{
	return powRgba(_rgba, 2.2);
}

vec4 toGamma(vec4 _rgba)
{
	return powRgba(_rgba, 1.0/2.2);
}

vec3 calcLight(int _idx, mat3 _tbn, vec3 _wpos, vec3 _normal, vec3 _view)
{
	vec3 lp = u_lightPosRadius[_idx].xyz - _wpos;
	float attn = 1.0 - smoothstep(u_lightRgbInnerR[_idx].w, 1.0, length(lp) / u_lightPosRadius[_idx].w);
	vec3 lightDir = mul(_tbn, normalize(lp) );
	vec2 bln = blinn(lightDir, _normal, _view);
	vec4 lc = lit(bln.x, bln.y, 1.0);
	vec3 rgb = u_lightRgbInnerR[_idx].xyz*max(0.0, saturate(lc.y) ) * attn;
	return rgb;
}

void main()
{
	mat3 tbn = mat3(v_tangent, cross(v_normal, v_tangent), v_normal);

	vec3 normal = normalize(2.0*texture2D(u_texNormal, v_texcoord0).xyz-1.0);

	vec3 lightColor;
	lightColor =  calcLight(0, tbn, v_wpos, normal, v_view);
	lightColor += calcLight(1, tbn, v_wpos, normal, v_view);
	lightColor += calcLight(2, tbn, v_wpos, normal, v_view);
	lightColor += calcLight(3, tbn, v_wpos, normal, v_view);

	vec4 color = toLinear(texture2D(u_texColor, v_texcoord0) );

	gl_FragColor.xyz = max(vec3_splat(0.05), lightColor.xyz)*color.xyz; // + fres*pow(lc.z, 128.0); //normal.xyz; //color.xyz; //*normal;
	gl_FragColor.w = 1.0;
	gl_FragColor = toGamma(gl_FragColor);
}