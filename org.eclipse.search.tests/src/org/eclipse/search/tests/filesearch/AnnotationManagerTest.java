/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.search.tests.filesearch;

import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AnnotationTypeLookup;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.search.tests.SearchTestPlugin;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import org.eclipse.search.internal.core.text.TextSearchScope;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;

import org.eclipse.search2.internal.ui.InternalSearchUI;

public class AnnotationManagerTest extends TestCase {
	FileSearchQuery fQuery1;
	FileSearchQuery fQuery2;

	private AnnotationTypeLookup fAnnotationTypeLookup= EditorsUI.getAnnotationTypeLookup();

	public AnnotationManagerTest(String name) {
		super(name);
	}
		
	protected void setUp() throws Exception {
		super.setUp();
		ZipFile zip= new ZipFile(SearchTestPlugin.getDefault().getFileInPlugin(new Path("testresources/junit37-noUI-src.zip"))); //$NON-NLS-1$
		
		SearchTestPlugin.importFilesFromZip(zip, new Path("Test"), null); //$NON-NLS-1$
		
		TextSearchScope scope= TextSearchScope.newWorkspaceScope();
		scope.addExtension("*.java");
		fQuery1= new FileSearchQuery(scope,  "", "Test", "Query1");
		fQuery2= new FileSearchQuery(scope, "", "TestCase", "Query2");
	}
	
	public void testAddAnnotation() throws Exception {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInForeground(null, fQuery1);
		AbstractTextSearchResult result= (AbstractTextSearchResult) fQuery1.getSearchResult();
		Object[] files= result.getElements();
		for (int i= 0; i < files.length; i++) {
			IFile file= (IFile) files[0];
			ITextEditor editor= (ITextEditor)IDE.openEditor(SearchTestPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
			IAnnotationModel annotationModel= editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
			annotationModel.getAnnotationIterator();
			HashSet positions= new HashSet();
			for (Iterator iter= annotationModel.getAnnotationIterator(); iter.hasNext();) {
				Annotation annotation= (Annotation) iter.next();
				if (annotation.getType().equals(fAnnotationTypeLookup.getAnnotationType(SearchUI.SEARCH_MARKER, IMarker.SEVERITY_INFO))) {
					positions.add(annotationModel.getPosition(annotation));
				}
			}

			Match[] matches= result.getMatches(file);
			for (int j= 0; j < matches.length; j++) {
				Position position= new Position(matches[j].getOffset(), matches[j].getLength());
				assertTrue("position not found at: "+j, positions.remove(position));
			}
			assertEquals(0, positions.size());
		
		}
	}
	
	public void testBogusAnnotation() throws Exception {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInForeground(null, fQuery1);
		FileSearchResult result= (FileSearchResult) fQuery1.getSearchResult();
		IFile file= (IFile) result.getElements()[0];
		IDE.openEditor(SearchTestPlugin.getDefault().getWorkbench().getWorkbenchWindows()[0].getPages()[0], file, true);
		result.addMatch(new Match(file, -1, -1));
	}
	
	public void testRemoveQuery() throws Exception {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInForeground(null, fQuery1);
		AbstractTextSearchResult result= (AbstractTextSearchResult) fQuery1.getSearchResult();
		Object[] files= result.getElements();
		InternalSearchUI.getInstance().removeQuery(fQuery1);
		for (int i= 0; i < files.length; i++) {
			IFile file= (IFile) files[0];
			ITextEditor editor= (ITextEditor)IDE.openEditor(SearchTestPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
			IAnnotationModel annotationModel= editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
			int annotationCount= 0;
			for (Iterator annotations= annotationModel.getAnnotationIterator(); annotations.hasNext();) {
				Annotation annotation= (Annotation) annotations.next();
				if (annotation.getType().equals(fAnnotationTypeLookup.getAnnotationType(SearchUI.SEARCH_MARKER, IMarker.SEVERITY_INFO))) {
					annotationCount++;
				}
			}
			assertEquals(0, annotationCount);
		}
	}

	
	public void testReplaceQuery() throws Exception {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInForeground(null, fQuery1);
		AbstractTextSearchResult result= (AbstractTextSearchResult) fQuery1.getSearchResult();
		Object[] files= result.getElements();
		NewSearchUI.runQueryInForeground(null, fQuery2);
		for (int i= 0; i < files.length; i++) {
			IFile file= (IFile) files[0];
			ITextEditor editor= (ITextEditor)IDE.openEditor(SearchTestPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
			IAnnotationModel annotationModel= editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
			int annotationCount= 0;
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			for (Iterator annotations= annotationModel.getAnnotationIterator(); annotations.hasNext();) {
				Annotation annotation= (Annotation) annotations.next();
				if (annotation.getType().equals(fAnnotationTypeLookup.getAnnotationType(SearchUI.SEARCH_MARKER, IMarker.SEVERITY_INFO))) {
					Position p= annotationModel.getPosition(annotation);
					String text= document.get(p.getOffset(), p.getLength());
					assertTrue(text.equalsIgnoreCase(fQuery2.getSearchString()));
				}	
			}
			assertEquals(0, annotationCount);
		}
	}

	public void testSwitchQuery() throws Exception {
		NewSearchUI.activateSearchResultView();
		NewSearchUI.runQueryInForeground(null, fQuery1);
		AbstractTextSearchResult result= (AbstractTextSearchResult) fQuery1.getSearchResult();
		Object[] files= result.getElements();
		NewSearchUI.runQueryInForeground(null, fQuery2);
		SearchTestPlugin.getDefault().getSearchView().showSearchResult(result);
		for (int i= 0; i < files.length; i++) {
			IFile file= (IFile) files[0];
			ITextEditor editor= (ITextEditor)IDE.openEditor(SearchTestPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
			IAnnotationModel annotationModel= editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
			int annotationCount= 0;
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			for (Iterator annotations= annotationModel.getAnnotationIterator(); annotations.hasNext();) {
				Annotation annotation= (Annotation) annotations.next();
				if (annotation.getType().equals(fAnnotationTypeLookup.getAnnotationType(SearchUI.SEARCH_MARKER, IMarker.SEVERITY_INFO))) {
					Position p= annotationModel.getPosition(annotation);
					String text= document.get(p.getOffset(), p.getLength());
					assertTrue(text.equalsIgnoreCase(fQuery1.getSearchString()));
				}	
			}
			assertEquals(0, annotationCount);
		}
	}

}
