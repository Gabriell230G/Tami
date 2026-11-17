"""estilo3.py · CSS global da Tami v3: estados, foco, movimento do rosto e movimento reduzido.
Entra no <helmet> de toda tela. Valores de tempo e curva vêm de tokens.MOV e tokens.CURVA.
"""
from tokens import COR, MOV, CURVA

C = COR
CSS = f"""
body{{margin:0}}
a{{color:{C['primary']}}}
a,button{{-webkit-tap-highlight-color:transparent;cursor:pointer;font:inherit}}
button{{border:0;background:none;padding:0;color:inherit}}
/* toque: o que tem sombra sólida afunda e a sombra some */
a:active,button:active,[role=button]:active{{transform:translateY(3px);box-shadow:none!important;transition:transform {MOV['toque']}ms {CURVA['padrao']}}}
/* foco: anel duplo, visível em fundo claro e escuro */
a:focus-visible,button:focus-visible,input:focus-visible,[tabindex]:focus-visible{{outline:3px solid {C['accent']};outline-offset:2px;box-shadow:0 0 0 6px {C['text']}!important}}
.t-raiz>*{{flex-shrink:0}}
@media (max-height: 700px){{.t-some-baixo{{display:none!important}}}}
.t-oculto{{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap;margin:0}}
/* rosto: piscar parado (a cada ~5 s, 150 ms) */
@keyframes t-pisca{{0%,96%,100%{{transform:scaleY(1)}}98%{{transform:scaleY(.12)}}}}
.t-anima-pisca .olho{{transform-box:fill-box;transform-origin:center;animation:t-pisca 5.2s infinite}}
.t-anima-pisca .olho+.olho{{animation-delay:0s}}
/* rosto: pensando (olhos sobem e vão para o lado, 600 ms cada sentido) */
@keyframes t-pensa{{0%,100%{{transform:translate(0,0)}}50%{{transform:translate(14px,-16px)}}}}
.t-anima-pensa .olho{{animation:t-pensa {MOV['pensa']*2}ms ease-in-out infinite}}
/* rosto: ouvindo (a boca vira onda) */
@keyframes t-onda{{0%,100%{{transform:scaleY(.3)}}50%{{transform:scaleY(1)}}}}
.t-anima-ouve .barra{{transform-box:fill-box;transform-origin:center;animation:t-onda 900ms ease-in-out infinite}}
.t-anima-ouve .barra:nth-of-type(2){{animation-delay:-150ms}}.t-anima-ouve .barra:nth-of-type(3){{animation-delay:-300ms}}
.t-anima-ouve .barra:nth-of-type(4){{animation-delay:-450ms}}.t-anima-ouve .barra:nth-of-type(5){{animation-delay:-600ms}}
/* resultado: a expressão chega antes do texto */
@keyframes t-sai{{0%,60%{{opacity:1}}100%{{opacity:0}}}}
@keyframes t-entra{{0%,60%{{opacity:0}}100%{{opacity:1}}}}
@keyframes t-sobe{{from{{opacity:0;transform:translateY(8px)}}to{{opacity:1;transform:none}}}}
.t-troca .antes{{animation:t-sai {MOV['resultado']*2}ms {CURVA['sai']} forwards}}
.t-troca .depois{{opacity:0;animation:t-entra {MOV['resultado']*2}ms {CURVA['entra']} forwards}}
.t-texto-entra{{opacity:0;animation:t-sobe {MOV['entrada']}ms {CURVA['entra']} {MOV['resultado']+MOV['texto_atraso']}ms forwards}}
/* conquista: um pulo, uma vez */
@keyframes t-pulo{{0%,100%{{transform:translateY(0)}}40%{{transform:translateY(-12%)}}}}
.t-anima-pulo{{animation:t-pulo 400ms {CURVA['entra']} 1}}
/* corpo: pulso de "escaneando" na parte marcada */
@keyframes t-pulso{{0%{{transform:scale(1);opacity:.9}}100%{{transform:scale(2.6);opacity:0}}}}
.t-pulso{{transform-box:fill-box;transform-origin:center;animation:t-pulso 1600ms {CURVA['entra']} infinite}}
/* menos movimento: tudo vira troca direta */
@media (prefers-reduced-motion: reduce){{*,*::before,*::after{{animation:none!important;transition:none!important}}.t-troca .antes{{display:none}}.t-troca .depois,.t-texto-entra{{opacity:1}}}}
"""
