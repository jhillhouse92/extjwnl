package net.sf.extjwnl.princeton.data;

import net.sf.extjwnl.JWNL;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.dictionary.Dictionary;
import net.sf.extjwnl.util.TokenizerParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * <code>FileDictionaryElementFactory</code> that parses lines from the dictionary files distributed by the
 * WordNet team at Princeton's Cognitive Science department.
 *
 * @author John Didion <jdidion@didion.net>
 * @author Aliaksandr Autayeu <avtaev@gmail.com>
 */
public abstract class AbstractPrincetonFileDictionaryElementFactory extends AbstractPrincetonDictionaryElementFactory implements FileDictionaryElementFactory {

    private static final Log log = LogFactory.getLog(AbstractPrincetonFileDictionaryElementFactory.class);

    protected AbstractPrincetonFileDictionaryElementFactory(Dictionary dictionary) {
        super(dictionary);
    }

    public IndexWord createIndexWord(POS pos, String line) throws JWNLException {
        TokenizerParser tokenizer = new TokenizerParser(line, " ");
        String lemma = tokenizer.nextToken().replace('_', ' ');
        tokenizer.nextToken(); // pos
        tokenizer.nextToken();    // sense_cnt

        int pointerCount = tokenizer.nextInt();
        for (int i = 0; i < pointerCount; ++i) {
            tokenizer.nextToken();    // ptr_symbol
        }
        //Same as sense_cnt above. This is redundant, but the field was preserved for compatibility reasons.
        int senseCount = tokenizer.nextInt();

        //Number of senses of lemma that are ranked according to their
        //frequency of occurrence in semantic concordance texts.
        tokenizer.nextInt(); // tagged sense count

        long[] synsetOffsets = new long[senseCount];
        for (int i = 0; i < senseCount; i++) {
            synsetOffsets[i] = tokenizer.nextLong();
        }
        if (log.isTraceEnabled()) {
            log.trace(JWNL.resolveMessage("PRINCETON_INFO_003", new Object[]{lemma, pos}));
        }
        return new IndexWord(dictionary, lemma, pos, synsetOffsets);
    }

    public Synset createSynset(POS pos, String line) throws JWNLException {
        TokenizerParser tokenizer = new TokenizerParser(line, " ");

        long offset = tokenizer.nextLong();
        long lexFileNum = tokenizer.nextLong();
        String synsetPOS = tokenizer.nextToken();

        Synset synset = new Synset(dictionary, POS.getPOSForKey(synsetPOS), offset);
        synset.setLexFileNum(lexFileNum);

        boolean isAdjectiveCluster = false;
        if ("s".equals(synsetPOS)) {
            isAdjectiveCluster = true;
        }
        synset.setIsAdjectiveCluster(isAdjectiveCluster);

        int wordCount = tokenizer.nextHexInt();
        for (int i = 0; i < wordCount; i++) {
            String lemma = tokenizer.nextToken().replace('_', ' ');

            int lexId = tokenizer.nextHexInt(); // lex id

            //NB index: Word numbers are assigned to the word fields in a synset, from left to right, beginning with 1
            Word w = createWord(synset, i + 1, lemma);
            w.setLexId(lexId);
            synset.getWords().add(w);
        }

        int pointerCount = tokenizer.nextInt();
        for (int i = 0; i < pointerCount; i++) {
            String pt = tokenizer.nextToken();
            PointerType pointerType = PointerType.getPointerTypeForKey(pt);
            long targetOffset = tokenizer.nextLong();
            POS targetPOS = POS.getPOSForKey(tokenizer.nextToken());
            int linkIndices = tokenizer.nextHexInt();
            int sourceIndex = linkIndices / 256;
            int targetIndex = linkIndices & 255;
            PointerTarget source = (sourceIndex == 0) ? synset : synset.getWords().get(sourceIndex - 1);

            Pointer p = new Pointer(source, pointerType, targetPOS, targetOffset, targetIndex);
            synset.getPointers().add(p);
        }

        if (pos == POS.VERB) {
            BitSet verbFrames = new BitSet();
            int verbFrameCount = tokenizer.nextInt();
            for (int i = 0; i < verbFrameCount; i++) {
                tokenizer.nextToken();    // "+"
                int frameNumber = tokenizer.nextInt();
                int wordIndex = tokenizer.nextHexInt();
                if (wordIndex > 0) {
                    ((MutableVerb) synset.getWords().get(wordIndex - 1)).setVerbFrameFlag(frameNumber);
                } else {
                    for (Word w : synset.getWords()) {
                        ((MutableVerb) w).setVerbFrameFlag(frameNumber);
                    }
                    verbFrames.set(frameNumber);
                }
            }
            synset.setVerbFrameFlags(verbFrames);
        }

        String gloss = null;
        int index = line.indexOf('|');
        if (index > 0) {
            //do not use trim, because some glosses have space before or space after
            //which changes offsets on load\save even without editing
            gloss = line.substring(index + 2, line.length() - 2);
        }
        synset.setGloss(gloss);

        if (log.isTraceEnabled()) {
            log.trace(JWNL.resolveMessage("PRINCETON_INFO_002", new Object[]{pos, offset}));
        }
        return synset;
    }

    public Exc createExc(POS pos, String line) throws JWNLException {
        StringTokenizer st = new StringTokenizer(line);
        String lemma = st.nextToken().replace('_', ' ');
        List<String> exceptions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            exceptions.add(st.nextToken().replace('_', ' '));
        }
        if (log.isTraceEnabled()) {
            log.trace(JWNL.resolveMessage("PRINCETON_INFO_001", new Object[]{pos, lemma}));
        }
        return new Exc(dictionary, pos, lemma, exceptions);
    }
}