/**
 * Ícones desenhados à mão em SVG, sem biblioteca.
 *
 * Todos herdam a cor do texto (currentColor) e o traço de 1.7 fica legível
 * tanto no menu recolhido quanto expandido.
 */

interface Props {
  className?: string;
}

function Base({ children, className }: Props & { children: React.ReactNode }) {
  return (
    <svg
      className={className}
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {children}
    </svg>
  );
}

export const IconePainel = (p: Props) => (
  <Base {...p}>
    <path d="M12 3a9 9 0 1 0 9 9h-9V3Z" />
    <path d="M15.5 3.5A9 9 0 0 1 20.5 8.5L15.5 10V3.5Z" />
  </Base>
);

export const IconeReceita = (p: Props) => (
  <Base {...p}>
    <path d="M12 19V5" />
    <path d="m5 12 7-7 7 7" />
    <path d="M4 21h16" />
  </Base>
);

export const IconeFixa = (p: Props) => (
  <Base {...p}>
    <path d="M17 2v4M7 2v4M3 9h18" />
    <rect x="3" y="4" width="18" height="17" rx="2.5" />
    <path d="M8 14h3M8 17.5h6" />
  </Base>
);

export const IconeVariavel = (p: Props) => (
  <Base {...p}>
    <path d="M3 5h2l2.2 10.2a2 2 0 0 0 2 1.6h7.4a2 2 0 0 0 2-1.5L20 8H6" />
    <circle cx="9.5" cy="20" r="1.2" />
    <circle cx="17" cy="20" r="1.2" />
  </Base>
);

export const IconeEconomia = (p: Props) => (
  <Base {...p}>
    <path d="M3 20V10M9 20V4M15 20v-7M21 20V7" />
  </Base>
);

export const IconeDivida = (p: Props) => (
  <Base {...p}>
    <rect x="2.5" y="5.5" width="19" height="13" rx="2.5" />
    <path d="M2.5 10h19" />
    <path d="M6 14.5h3" />
  </Base>
);

export const IconeMenu = (p: Props) => (
  <Base {...p}>
    <path d="M4 7h16M4 12h16M4 17h16" />
  </Base>
);

export const IconeFechar = (p: Props) => (
  <Base {...p}>
    <path d="M6 6l12 12M18 6L6 18" />
  </Base>
);

export const IconeSair = (p: Props) => (
  <Base {...p}>
    <path d="M9 21H5.5A2.5 2.5 0 0 1 3 18.5v-13A2.5 2.5 0 0 1 5.5 3H9" />
    <path d="M16 17l5-5-5-5" />
    <path d="M21 12H9" />
  </Base>
);

export const IconeSol = (p: Props) => (
  <Base {...p}>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
  </Base>
);

export const IconeLua = (p: Props) => (
  <Base {...p}>
    <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z" />
  </Base>
);

export const IconeSetaEsquerda = (p: Props) => (
  <Base {...p}>
    <path d="M19 12H5" />
    <path d="m11 18-6-6 6-6" />
  </Base>
);

export const IconeSetaDireita = (p: Props) => (
  <Base {...p}>
    <path d="M5 12h14" />
    <path d="m13 6 6 6-6 6" />
  </Base>
);
