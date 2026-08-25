import { useEffect, useRef, useState } from 'react';

const preferindoMenosMovimento = () =>
  window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * Anima um número do valor anterior até o novo.
 *
 * Serve para o dinheiro não "pular" quando a pessoa troca de mês: o olho
 * acompanha para onde o total foi. Quem pediu menos movimento no sistema
 * recebe o valor final direto, sem animação.
 */
export function useContagem(valor: number, duracao = 650): number {
  const [atual, setAtual] = useState(valor);
  const anterior = useRef(valor);

  useEffect(() => {
    if (preferindoMenosMovimento() || anterior.current === valor) {
      setAtual(valor);
      anterior.current = valor;
      return;
    }

    const de = anterior.current;
    const inicio = performance.now();
    let quadro = 0;

    const passo = (agora: number) => {
      const t = Math.min((agora - inicio) / duracao, 1);
      // easeOutCubic: rápido no começo, assentando no fim.
      const suave = 1 - (1 - t) ** 3;
      setAtual(de + (valor - de) * suave);

      if (t < 1) {
        quadro = requestAnimationFrame(passo);
      } else {
        anterior.current = valor;
      }
    };

    quadro = requestAnimationFrame(passo);
    return () => cancelAnimationFrame(quadro);
  }, [valor, duracao]);

  return atual;
}
