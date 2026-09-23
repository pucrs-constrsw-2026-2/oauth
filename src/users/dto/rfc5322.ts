/**
 * Forma `dot-atom` de endereço da RFC 5322: parte local de átomos separados
 * por ponto, domínio com ao menos um rótulo e TLD alfabético.
 *
 * Recusa `ana@@pucrs.br`, `ana@pucrs` e `.ana@pucrs.br`.
 */
export const RFC_5322_EMAIL =
  /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/;

export const RFC_5322_MESSAGE =
  "email deve ser um endereço válido conforme a RFC 5322";
