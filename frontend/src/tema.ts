import { useEffect, useState } from 'react';

export type Tema = 'claro' | 'escuro';

const CHAVE = 'despesas.tema';

/**
 * O tema começa claro e é escolhido pela pessoa, não herdado do sistema.
 *
 * localStorage pode estourar (janela anônima, cookies bloqueados), então
 * leitura e escrita ficam protegidas: no pior caso o app abre no claro.
 */
function inicial(): Tema {
  try {
    const guardado = localStorage.getItem(CHAVE);
    if (guardado === 'claro' || guardado === 'escuro') {
      return guardado;
    }
  } catch {
    // segue com o padrão
  }
  return 'claro';
}

export function useTema(): [Tema, () => void] {
  const [tema, setTema] = useState<Tema>(inicial);

  useEffect(() => {
    document.documentElement.dataset.tema = tema;
    try {
      localStorage.setItem(CHAVE, tema);
    } catch {
      // sem persistência, mas a troca vale para esta sessão
    }
  }, [tema]);

  return [tema, () => setTema((atual) => (atual === 'claro' ? 'escuro' : 'claro'))];
}
