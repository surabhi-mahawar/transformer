package com.uci.transformer.odk;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.utils.DefaultAnswerResolver;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormAnswerDataSerializer;

import java.util.ArrayList;
import java.util.List;

public class ExternalAnswerResolver extends DefaultAnswerResolver {

    @Override
    public IAnswerData resolveAnswer(String textVal, TreeElement treeElement, FormDef formDef) {
        QuestionDef questionDef = XFormParser.ghettoGetQuestionDef(treeElement.getDataType(),
                formDef, treeElement.getRef());
        if (questionDef != null && (questionDef.getControlType() == Constants.CONTROL_SELECT_ONE
                || questionDef.getControlType() == Constants.CONTROL_SELECT_MULTI)) {
                List<SelectChoice> staticChoices;
                try{
                    staticChoices = questionDef.getChoices();
                    if(staticChoices == null){
                        formDef.populateDynamicChoices(questionDef.getDynamicChoices(), treeElement.getRef());
                        staticChoices = questionDef.getDynamicChoices().getChoices();
                    }
                }catch(Exception e){
                    System.out.println(e);
                    staticChoices = new ArrayList<>();
                }
                for (int index = 0; index < staticChoices.size(); index++) {
                    SelectChoice selectChoice = staticChoices.get(index);
                    String selectChoiceValue = selectChoice.getValue();
                    switch (questionDef.getControlType()) {
                        case Constants.CONTROL_SELECT_ONE: {
                            // the default implementation will search for the "textVal"
                            // (saved answer) inside the static choices.
                            // Since we know that there isn't such, we just wrap the textVal
                            // in a virtual choice in order to
                            // create a SelectOneData object to be used as the IAnswer to the
                            // TreeElement.
                            // (the caller of this function is searching for such an answer
                            // to populate the in-memory model.)
                            SelectChoice customSelectChoice = new SelectChoice(textVal, textVal,
                                    false);
                            customSelectChoice.setIndex(index);
                            return new SelectOneData(customSelectChoice.selection());
                        }
                        case Constants.CONTROL_SELECT_MULTI: {
                            // we should create multiple selections and add them to the pile
                            List<SelectChoice> customSelectChoices = createCustomSelectChoices(
                                    textVal);
                            List<Selection> customSelections = new ArrayList<>();
                            for (SelectChoice customSelectChoice : customSelectChoices) {
                                customSelections.add(customSelectChoice.selection());
                            }
                            return new SelectMultiData(customSelections);
                        }
                        default: {
                            // There is a bug if we get here, so let's throw an Exception
                            throw createBugRuntimeException(treeElement, textVal);
                        }
                    }
                }

        }
        // default behavior matches original behavior (for static selects, etc.)
        return super.resolveAnswer(textVal, treeElement, formDef);
    }

    private RuntimeException createBugRuntimeException(TreeElement treeElement, String textVal) {
        return new RuntimeException("The appearance column of the field " + treeElement.getName()
                + " contains a search() call and the field type is " + treeElement.getDataType()
                + " and the saved answer is " + textVal);
    }

    protected List<SelectChoice> createCustomSelectChoices(String completeTextValue) {
        // copied from org.javarosa.xform.util.XFormAnswerDataParser.getSelections()
        List<String> textValues = DateUtils.split(completeTextValue,
                XFormAnswerDataSerializer.DELIMITER, true);

        int index = 0;
        List<SelectChoice> customSelectChoices = new ArrayList<>();
        for (String textValue : textValues) {
            SelectChoice selectChoice = new SelectChoice(textValue, textValue, false);
            selectChoice.setIndex(index++);
            customSelectChoices.add(selectChoice);
        }

        return customSelectChoices;
    }
}