package net.didion.jwnl.data;

import net.didion.jwnl.JWNLException;
import net.didion.jwnl.dictionary.Dictionary;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Tests editing the FileBackedDictionary.
 *
 * @author Aliaksandr Autayeu avtaev@gmail.com
 */
public class TestFileBackedDictionaryCreate {

    protected static final String properties = "./config/clean_properties.xml";

    private final String entityGloss = "that which is perceived or known or inferred to have its own distinct existence (living or nonliving)";
    private final String entityLemma = "entity";
    private final String physical_entityGloss = "an entity that has physical existence";
    private final String physical_entityLemma = "physìcal_entìty";//ì to test encoding

    private final String[] exception1 = {"alto-relievos", "alto-relievo", "alto-rilievo"};
    private final String[] exception2 = {"aìdes-de-camp", "aìde-de-camp"};//ì to test encoding
    private final String[] exception3 = {"altocumuli", "altocumulus"};//to test sorting

    private Dictionary dictionary;

    private Synset synEntity;
    private ArrayList<Synset> synsets;
    private Iterator<Synset> si;
    private IndexWord iwEntity;
    private ArrayList<IndexWord> indexWords;
    private Iterator<IndexWord> ii;
    private Synset synPEntity;
    private IndexWord iwPEntity;
    private Exc e1;
    private Exc e2;
    private Exc e3;

    @Before
    public void setUp() throws IOException, JWNLException {
        dictionary = Dictionary.getInstance(new FileInputStream(properties));
        dictionary.delete();
        dictionary = Dictionary.getInstance(new FileInputStream(properties));

        synEntity = null;
        synsets = null;
        si = null;
        iwEntity = null;
        indexWords = null;
        ii = null;
        synPEntity = null;
        iwPEntity = null;
        e1 = null;
        e2 = null;
        e3 = null;
    }

    @After
    public void tearDown() throws IOException, JWNLException {
        dictionary.close();
        //to ensure folder remains clean
        dictionary = Dictionary.getInstance(new FileInputStream(properties));
        dictionary.delete();
    }

    @Test
    public void TestEmptyDictionary() throws IOException, JWNLException {
        for (POS pos : POS.getAllPOS()) {
            ArrayList<Synset> synsets = new ArrayList<Synset>();
            Iterator<Synset> si = dictionary.getSynsetIterator(pos);
            while (si.hasNext()) {
                synsets.add(si.next());
            }
            Assert.assertEquals(0, synsets.size());

            ArrayList<IndexWord> indexWords = new ArrayList<IndexWord>();
            Iterator<IndexWord> ii = dictionary.getIndexWordIterator(pos);
            while (ii.hasNext()) {
                indexWords.add(ii.next());
            }
            Assert.assertEquals(0, indexWords.size());

            ArrayList<Exc> exceptions = new ArrayList<Exc>();
            Iterator<Exc> ei = dictionary.getExceptionIterator(pos);
            while (ei.hasNext()) {
                exceptions.add(ei.next());
            }
            Assert.assertEquals(0, exceptions.size());
        }
    }

    @Test
    public void TestExceptions() throws JWNLException {
        dictionary.edit();

        createExceptions(dictionary);
        testExceptions(dictionary);
    }

    @Test
    public void TestExceptionsRecreate() throws JWNLException, IOException {
        dictionary.edit();

        createExceptions(dictionary);
        testExceptions(dictionary);

        dictionary.save();
        dictionary.close();

        dictionary = Dictionary.getInstance(new FileInputStream(properties));

        e1 = dictionary.getException(POS.NOUN, exception1[0]);
        e2 = dictionary.getException(POS.NOUN, exception1[0]);
        e3 = dictionary.getException(POS.NOUN, exception1[0]);

        testExceptions(dictionary);
    }

    private void createExceptions(Dictionary dictionary) throws JWNLException {
        e1 = dictionary.createException(POS.NOUN, exception1[0], Arrays.asList(exception1[1], exception1[2]));
        e2 = dictionary.createException(POS.NOUN, exception2[0], Arrays.asList(exception2[1]));
        e3 = dictionary.createException(POS.NOUN, exception3[0], Arrays.asList(exception3[1]));
    }

    private void testExceptions(Dictionary dictionary) throws JWNLException {
        Assert.assertNotNull(e1);
        Assert.assertEquals(exception1[0], e1.getLemma());
        Assert.assertEquals(2, e1.getExceptions().size());
        Assert.assertEquals(exception1[1], e1.getExceptions().get(0));
        Assert.assertEquals(exception1[2], e1.getExceptions().get(1));
        Assert.assertNotNull(e2);
        Assert.assertNotNull(e3);

        List<Exc> exceptions = new ArrayList<Exc>(3);
        Iterator<Exc> ei = dictionary.getExceptionIterator(POS.NOUN);
        while (ei.hasNext()) {
            exceptions.add(ei.next());
        }
        Assert.assertEquals(3, exceptions.size());
        Assert.assertTrue(exceptions.contains(e1));
        Assert.assertTrue(exceptions.contains(e2));
        Assert.assertTrue(exceptions.contains(e3));
    }

    @Test
    public void TestCreateEntitySynset() throws JWNLException {
        dictionary.edit();
        createAndTestEntitySynset(dictionary);
    }

    private void createAndTestEntitySynset(Dictionary dictionary) throws JWNLException {
        synEntity = dictionary.createSynset(POS.NOUN);
        Assert.assertNotNull(synEntity);
        Assert.assertEquals(POS.NOUN, synEntity.getPOS());
        Assert.assertTrue(-1 < synEntity.getOffset());
        Assert.assertEquals(0, synEntity.getPointers().size());
        Assert.assertEquals(0, synEntity.getWords().size());

        synsets = new ArrayList<Synset>();
        si = dictionary.getSynsetIterator(POS.NOUN);
        while (si.hasNext()) {
            synsets.add(si.next());
        }
        Assert.assertEquals(1, synsets.size());
        Assert.assertTrue(synsets.contains(synEntity));

        synEntity.setGloss(entityGloss);
        Assert.assertEquals(entityGloss, synEntity.getGloss());
    }

    @Test
    public void TestCreateEntityWord() throws JWNLException {
        dictionary.edit();
        createAndTestEntitySynset(dictionary);
        createAndTestEntityWord(dictionary);
    }

    private void createAndTestEntityWord(Dictionary dictionary) throws JWNLException {
        synEntity.getWords().add(new Word(dictionary, synEntity, 1, entityLemma));
        Assert.assertEquals(1, synEntity.getWords().size());
        Assert.assertNotNull(synEntity.getWords().get(0));
        Assert.assertEquals(entityLemma, synEntity.getWords().get(0).getLemma());
        Assert.assertEquals(1, synEntity.getWords().get(0).getIndex());
        Assert.assertEquals(POS.NOUN, synEntity.getWords().get(0).getPOS());
        Assert.assertEquals(synEntity, synEntity.getWords().get(0).getSynset());

        iwEntity = dictionary.getIndexWord(POS.NOUN, entityLemma);
        Assert.assertNotNull(iwEntity);
        Assert.assertEquals(1, iwEntity.getSenses().size());
        Assert.assertEquals(synEntity, iwEntity.getSenses().get(0));
        Assert.assertEquals(entityLemma, iwEntity.getLemma());
        Assert.assertEquals(POS.NOUN, iwEntity.getPOS());
        Assert.assertNotNull(iwEntity.getSynsetOffsets());
        Assert.assertEquals(1, iwEntity.getSynsetOffsets().length);
        Assert.assertEquals(synEntity.getOffset(), iwEntity.getSynsetOffsets()[0]);

        ArrayList<IndexWord> indexWords = new ArrayList<IndexWord>();
        Iterator<IndexWord> ii = dictionary.getIndexWordIterator(POS.NOUN);
        while (ii.hasNext()) {
            indexWords.add(ii.next());
        }
        Assert.assertEquals(1, indexWords.size());
        Assert.assertTrue(indexWords.contains(iwEntity));
    }

    @Test
    public void TestCreatePEntitySynset() throws JWNLException {
        dictionary.edit();
        createAndTestPEntitySynset(dictionary);
    }

    private void createAndTestPEntitySynset(Dictionary dictionary) throws JWNLException {
        synPEntity = dictionary.createSynset(POS.NOUN);
        Assert.assertNotNull(synPEntity);
        synPEntity.setGloss(physical_entityGloss);
    }

    @Test
    public void TestCreatePEntityWord() throws JWNLException {
        dictionary.edit();
        createAndTestEntitySynset(dictionary);
        createAndTestPEntitySynset(dictionary);
        createAndTestPEntityWord(dictionary);
    }

    private void createAndTestPEntityWord(Dictionary dictionary) throws JWNLException {
        synPEntity.getWords().add(new Word(dictionary, synEntity, 1, physical_entityLemma));
        iwPEntity = dictionary.getIndexWord(POS.NOUN, physical_entityLemma);
        Assert.assertNotNull(iwPEntity);
        Assert.assertEquals(1, iwPEntity.getSenses().size());
        Assert.assertEquals(synPEntity, iwPEntity.getSenses().get(0));
        Assert.assertEquals(physical_entityLemma, iwPEntity.getLemma());
        Assert.assertEquals(POS.NOUN, iwPEntity.getPOS());
        Assert.assertNotNull(iwPEntity.getSynsetOffsets());
        Assert.assertEquals(1, iwPEntity.getSynsetOffsets().length);
        Assert.assertEquals(synPEntity.getOffset(), iwPEntity.getSynsetOffsets()[0]);
    }

    @Test
    public void TestCreatePEntityPointer() throws JWNLException {
        dictionary.edit();
        createAndTestEntitySynset(dictionary);
        createAndTestPEntitySynset(dictionary);
        createAndTestPEntityPointer(dictionary);
    }

    private void createAndTestPEntityPointer(Dictionary dictionary) {
        synPEntity.getPointers().add(new Pointer(PointerType.HYPERNYM, synPEntity, synEntity));
        testEntityPEntityPointers(dictionary);
    }

    private void testEntityPEntityPointers(Dictionary dictionary) {
        //direct pointer
        Assert.assertEquals(1, synPEntity.getPointers().size());
        Assert.assertNotNull(synPEntity.getPointers().get(0));
        Assert.assertEquals(PointerType.HYPERNYM, synPEntity.getPointers().get(0).getType());
        Assert.assertEquals(synPEntity, synPEntity.getPointers().get(0).getSource());
        Assert.assertEquals(synEntity, synPEntity.getPointers().get(0).getTarget());

        //reverse pointer
        Assert.assertEquals(1, synEntity.getPointers().size());
        Assert.assertNotNull(synEntity.getPointers().get(0));
        Assert.assertEquals(PointerType.HYPONYM, synEntity.getPointers().get(0).getType());
        Assert.assertEquals(synEntity, synEntity.getPointers().get(0).getSource());
        Assert.assertEquals(synPEntity, synEntity.getPointers().get(0).getTarget());
    }

    @Test
    public void TestComplexEdit() throws IOException, JWNLException {
        dictionary.edit();

        createAndTestEntitySynset(dictionary);
        createAndTestEntityWord(dictionary);

        createAndTestPEntitySynset(dictionary);
        createAndTestPEntityWord(dictionary);

        createAndTestPEntityPointer(dictionary);
        testIterators(dictionary);
    }

    @Test
    public void TestComplexEditAndSave() throws IOException, JWNLException {
        dictionary.edit();
        createAndTestEntitySynset(dictionary);
        createAndTestEntityWord(dictionary);

        createAndTestPEntitySynset(dictionary);
        createAndTestPEntityWord(dictionary);

        createAndTestPEntityPointer(dictionary);
        testIterators(dictionary);

        dictionary.save();
    }

    @Test
    public void TestEditSaveLoad() throws IOException, JWNLException {
        dictionary.edit();
        createAndTestEntitySynset(dictionary);
        createAndTestEntityWord(dictionary);

        createAndTestPEntitySynset(dictionary);
        createAndTestPEntityWord(dictionary);

        createAndTestPEntityPointer(dictionary);
        testIterators(dictionary);

        dictionary.save();

        dictionary = Dictionary.getInstance(new FileInputStream(properties));

        testIWEntity(dictionary);
        testIWPEntity(dictionary);
        testEntityPEntityPointers(dictionary);
        testIterators(dictionary);
    }

    private void testIWPEntity(Dictionary dictionary) throws JWNLException {
        iwPEntity = dictionary.getIndexWord(POS.NOUN, physical_entityLemma);
        Assert.assertNotNull(iwPEntity);
        Assert.assertEquals(physical_entityLemma, iwPEntity.getLemma());
        Assert.assertEquals(1, iwPEntity.getSenses().size());

        synPEntity = iwPEntity.getSenses().get(0);
        Assert.assertNotNull(synPEntity);
    }

    private void testIWEntity(Dictionary dictionary) throws JWNLException {
        iwEntity = dictionary.getIndexWord(POS.NOUN, entityLemma);
        Assert.assertNotNull(iwEntity);
        Assert.assertEquals(entityLemma, iwEntity.getLemma());
        Assert.assertEquals(1, iwEntity.getSenses().size());

        synEntity = iwEntity.getSenses().get(0);
        Assert.assertNotNull(synEntity);
    }

    private void testIterators(Dictionary dictionary) throws JWNLException {
        synsets = new ArrayList<Synset>();
        si = dictionary.getSynsetIterator(POS.NOUN);
        while (si.hasNext()) {
            synsets.add(si.next());
        }
        Assert.assertEquals(2, synsets.size());
        Assert.assertTrue(synsets.contains(synEntity));
        Assert.assertTrue(synsets.contains(synPEntity));

        indexWords = new ArrayList<IndexWord>();
        ii = dictionary.getIndexWordIterator(POS.NOUN);
        while (ii.hasNext()) {
            indexWords.add(ii.next());
        }
        Assert.assertEquals(2, indexWords.size());
        Assert.assertTrue(indexWords.contains(iwEntity));
        Assert.assertTrue(indexWords.contains(iwPEntity));

    }
}