package org.rkg.chase;

import java.nio.charset.StandardCharsets;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;

/**
 * Deterministic Skolem witness naming scheme for rule 22/23 witnesses (§3.3 of the software
 * design document): percent-encoding of the full source IRI, chosen over a content hash (e.g.
 * SHA-1) specifically for human readability while remaining deterministic and injective.
 *
 * <p>Naming scheme:
 * <ul>
 *   <li>{@code s_a  = urn:rkg:witness:class:{percent-encode(a)}}</li>
 *   <li>{@code s'_p = urn:rkg:witness:prop:src:{percent-encode(p)}}</li>
 *   <li>{@code s''_p = urn:rkg:witness:prop:tgt:{percent-encode(p)}}</li>
 * </ul>
 * Blank-node classes/properties use the store's own stable blank-node label instead, prefixed
 * with {@code bn:} (e.g. {@code urn:rkg:witness:class:bn:b0}).
 */
public final class SkolemNaming {

    private static final String NAMESPACE = "urn:rkg:witness:";

    private SkolemNaming() {
    }

    /**
     * {@code s_a} — the witness minted for rule 22 given a populated class {@code classTerm}.
     *
     * @param classTerm class IRI or blank node
     * @param isBlankNode whether classTerm is a blank node
     * @return witness IRI
     * @see Rdf4jChaseOrchestrator
     * @see <a href="docs/software-design-document.md">Design Document &#167;3.3</a>
     */
    public static String classWitness(String classTerm, boolean isBlankNode) {
        return NAMESPACE + "class:" + encodeTerm(classTerm, isBlankNode);
    }

    /**
     * {@code s_a} — the witness minted for an RDF4J class term.
     *
     * @param classTerm class IRI or blank node
     * @return witness IRI
     */
    public static String classWitness(Value classTerm) {
        return NAMESPACE + "class:" + encodeTerm(classTerm);
    }

    /**
     * {@code s'_p} — the source-side witness minted for rule 23 given a populated property.
     *
     * @param propertyTerm property IRI or blank node
     * @param isBlankNode whether propertyTerm is a blank node
     * @return source-side witness IRI
     * @see Rdf4jChaseOrchestrator
     * @see <a href="docs/software-design-document.md">Design Document &#167;3.3</a>
     */
    public static String propertySourceWitness(String propertyTerm, boolean isBlankNode) {
        return NAMESPACE + "prop:src:" + encodeTerm(propertyTerm, isBlankNode);
    }

    /**
     * {@code s'_p} — the source-side witness minted for an RDF4J property term.
     *
     * @param propertyTerm property IRI or blank node
     * @return source-side witness IRI
     */
    public static String propertySourceWitness(Value propertyTerm) {
        return NAMESPACE + "prop:src:" + encodeTerm(propertyTerm);
    }

    /**
     * {@code s''_p} — the target-side witness minted for rule 23 given a populated property.
     *
     * @param propertyTerm property IRI or blank node
     * @param isBlankNode whether propertyTerm is a blank node
     * @return target-side witness IRI
     * @see Rdf4jChaseOrchestrator
     * @see <a href="docs/software-design-document.md">Design Document &#167;3.3</a>
     */
    public static String propertyTargetWitness(String propertyTerm, boolean isBlankNode) {
        return NAMESPACE + "prop:tgt:" + encodeTerm(propertyTerm, isBlankNode);
    }

    /**
     * {@code s''_p} — the target-side witness minted for an RDF4J property term.
     *
     * @param propertyTerm property IRI or blank node
     * @return target-side witness IRI
     */
    public static String propertyTargetWitness(Value propertyTerm) {
        return NAMESPACE + "prop:tgt:" + encodeTerm(propertyTerm);
    }

    private static String encodeTerm(String term, boolean isBlankNode) {
        return isBlankNode ? "bn:" + term : percentEncode(term);
    }

    private static String encodeTerm(Value term) {
        if (term instanceof BNode blankNode) {
            // This stable identifier names the Skolem IRI only; the BNode itself is passed to
            // GraphDB through RDF4J bindings and is never reconstructed from this string.
            return "bn:" + blankNode.getID();
        }
        return percentEncode(term.stringValue());
    }

    /**
     * RFC 3986 percent-encoding of an IRI, preserving unreserved characters
     * ({@code A-Z a-z 0-9 - _ . ~}) and percent-encoding everything else, including the
     * sub-delimiters and gen-delims that would otherwise be ambiguous inside our own
     * {@code urn:rkg:witness:...} path segments (notably {@code :} and {@code /}).
     *
     * @param iri IRI to encode
     * @return percent-encoded IRI
     */
    public static String percentEncode(String iri) {
        StringBuilder out = new StringBuilder(iri.length() * 2);
        for (byte b : iri.getBytes(StandardCharsets.UTF_8)) {
            int unsigned = b & 0xFF;
            char c = (char) unsigned;
            boolean unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~';
            if (unreserved) {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", unsigned));
            }
        }
        return out.toString();
    }

    /**
     * Inverse of {@link #percentEncode}, for {@code --explain}/decoding a witness IRI back to its source.
     *
     * @param encoded percent-encoded IRI
     * @return decoded IRI
     */
    public static String percentDecode(String encoded) {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream(encoded.length());
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '%' && i + 2 < encoded.length()) {
                int value = Integer.parseInt(encoded.substring(i + 1, i + 3), 16);
                bytes.write(value);
                i += 2;
            } else {
                bytes.write(c);
            }
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
}
