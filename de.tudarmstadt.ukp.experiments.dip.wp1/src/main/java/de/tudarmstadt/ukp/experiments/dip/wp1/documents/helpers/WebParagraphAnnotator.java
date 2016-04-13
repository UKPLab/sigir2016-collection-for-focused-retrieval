/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.experiments.dip.wp1.documents.helpers;

import de.tudarmstadt.ukp.experiments.dip.wp1.documents.WebParagraph;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class WebParagraphAnnotator
        extends JCasAnnotator_ImplBase
{
    private static final String NEWLINE_CHARACTER = "\n";

    @Override
    public void process(JCas aJCas)
            throws AnalysisEngineProcessException
    {
        List<Integer> newLineIndices = findAllNewLineIndices(aJCas.getDocumentText());

        Iterator<Integer> iterator = newLineIndices.iterator();
        int previousIndex = 0;
        while (iterator.hasNext()) {
            Integer currentIndex = iterator.next();
            // label paragraph
            WebParagraph p = new WebParagraph(aJCas, previousIndex, currentIndex);
            p.addToIndexes();

            previousIndex = currentIndex + 1;
        }

        // last paragraph
        WebParagraph p = new WebParagraph(aJCas, previousIndex, aJCas.getDocumentText().length());
        p.addToIndexes();
    }

    /**
     * Returns a list of all indices of "\n" character in string
     *
     * @param s string
     * @return list (may be empty, never null)
     */
    static List<Integer> findAllNewLineIndices(String s)
    {
        List<Integer> result = new ArrayList<>();
        int i = -1;
        while ((i = s.indexOf(NEWLINE_CHARACTER, i + 1)) != -1) {
            result.add(i);
        }

        return result;
    }
}
