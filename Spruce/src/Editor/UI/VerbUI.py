# -*- coding: utf-8 -*-

# Form implementation generated from reading ui file 'Verb.ui'
#
# Created: Sun Jul 28 01:23:05 2013
#      by: PyQt4 UI code generator 4.10
#
# WARNING! All changes made in this file will be lost!

from PyQt4 import QtCore, QtGui

try:
    _fromUtf8 = QtCore.QString.fromUtf8
except AttributeError:
    def _fromUtf8(s):
        return s

try:
    _encoding = QtGui.QApplication.UnicodeUTF8
    def _translate(context, text, disambig):
        return QtGui.QApplication.translate(context, text, disambig, _encoding)
except AttributeError:
    def _translate(context, text, disambig):
        return QtGui.QApplication.translate(context, text, disambig)

class Ui_Form(object):
    def setupUi(self, Form):
        Form.setObjectName(_fromUtf8("Form"))
        Form.resize(714, 522)
        self.horizontalLayout_6 = QtGui.QHBoxLayout(Form)
        self.horizontalLayout_6.setObjectName(_fromUtf8("horizontalLayout_6"))
        self.verticalFrame = QtGui.QFrame(Form)
        sizePolicy = QtGui.QSizePolicy(QtGui.QSizePolicy.Fixed, QtGui.QSizePolicy.Preferred)
        sizePolicy.setHorizontalStretch(200)
        sizePolicy.setVerticalStretch(0)
        sizePolicy.setHeightForWidth(self.verticalFrame.sizePolicy().hasHeightForWidth())
        self.verticalFrame.setSizePolicy(sizePolicy)
        self.verticalFrame.setMaximumSize(QtCore.QSize(200, 16777215))
        self.verticalFrame.setObjectName(_fromUtf8("verticalFrame"))
        self.verticalLayout = QtGui.QVBoxLayout(self.verticalFrame)
        self.verticalLayout.setMargin(0)
        self.verticalLayout.setObjectName(_fromUtf8("verticalLayout"))
        self.label = QtGui.QLabel(self.verticalFrame)
        self.label.setMaximumSize(QtCore.QSize(200, 16777215))
        self.label.setObjectName(_fromUtf8("label"))
        self.verticalLayout.addWidget(self.label)
        self.lWords = QtGui.QListWidget(self.verticalFrame)
        self.lWords.setMaximumSize(QtCore.QSize(200, 16777215))
        self.lWords.setObjectName(_fromUtf8("lWords"))
        self.verticalLayout.addWidget(self.lWords)
        self.bRemove = QtGui.QPushButton(self.verticalFrame)
        sizePolicy = QtGui.QSizePolicy(QtGui.QSizePolicy.Fixed, QtGui.QSizePolicy.Fixed)
        sizePolicy.setHorizontalStretch(0)
        sizePolicy.setVerticalStretch(0)
        sizePolicy.setHeightForWidth(self.bRemove.sizePolicy().hasHeightForWidth())
        self.bRemove.setSizePolicy(sizePolicy)
        self.bRemove.setObjectName(_fromUtf8("bRemove"))
        self.verticalLayout.addWidget(self.bRemove)
        self.horizontalLayout_6.addWidget(self.verticalFrame)
        self.line = QtGui.QFrame(Form)
        self.line.setFrameShape(QtGui.QFrame.VLine)
        self.line.setFrameShadow(QtGui.QFrame.Sunken)
        self.line.setObjectName(_fromUtf8("line"))
        self.horizontalLayout_6.addWidget(self.line)
        self.gridLayout = QtGui.QGridLayout()
        self.gridLayout.setObjectName(_fromUtf8("gridLayout"))
        self.singularLabel = QtGui.QLabel(Form)
        self.singularLabel.setObjectName(_fromUtf8("singularLabel"))
        self.gridLayout.addWidget(self.singularLabel, 0, 0, 1, 1)
        self.tPresent = QtGui.QLineEdit(Form)
        self.tPresent.setObjectName(_fromUtf8("tPresent"))
        self.gridLayout.addWidget(self.tPresent, 0, 1, 1, 1)
        self.singularSyllablesLabel = QtGui.QLabel(Form)
        self.singularSyllablesLabel.setObjectName(_fromUtf8("singularSyllablesLabel"))
        self.gridLayout.addWidget(self.singularSyllablesLabel, 1, 0, 1, 1)
        self.presentSyl = QtGui.QSpinBox(Form)
        self.presentSyl.setMaximumSize(QtCore.QSize(60, 16777215))
        self.presentSyl.setObjectName(_fromUtf8("presentSyl"))
        self.gridLayout.addWidget(self.presentSyl, 1, 1, 1, 1)
        spacerItem = QtGui.QSpacerItem(20, 20, QtGui.QSizePolicy.Minimum, QtGui.QSizePolicy.Fixed)
        self.gridLayout.addItem(spacerItem, 2, 1, 1, 1)
        self.pluralLabel = QtGui.QLabel(Form)
        self.pluralLabel.setObjectName(_fromUtf8("pluralLabel"))
        self.gridLayout.addWidget(self.pluralLabel, 3, 0, 1, 1)
        self.tPart = QtGui.QLineEdit(Form)
        self.tPart.setObjectName(_fromUtf8("tPart"))
        self.gridLayout.addWidget(self.tPart, 3, 1, 1, 1)
        self.pluralSyllablesLabel = QtGui.QLabel(Form)
        self.pluralSyllablesLabel.setObjectName(_fromUtf8("pluralSyllablesLabel"))
        self.gridLayout.addWidget(self.pluralSyllablesLabel, 4, 0, 1, 1)
        self.partSyl = QtGui.QSpinBox(Form)
        self.partSyl.setMaximumSize(QtCore.QSize(60, 16777215))
        self.partSyl.setObjectName(_fromUtf8("partSyl"))
        self.gridLayout.addWidget(self.partSyl, 4, 1, 1, 1)
        spacerItem1 = QtGui.QSpacerItem(20, 20, QtGui.QSizePolicy.Minimum, QtGui.QSizePolicy.Fixed)
        self.gridLayout.addItem(spacerItem1, 5, 1, 1, 1)
        self.pastLabel = QtGui.QLabel(Form)
        self.pastLabel.setObjectName(_fromUtf8("pastLabel"))
        self.gridLayout.addWidget(self.pastLabel, 6, 0, 1, 1)
        self.tPast = QtGui.QLineEdit(Form)
        self.tPast.setObjectName(_fromUtf8("tPast"))
        self.gridLayout.addWidget(self.tPast, 6, 1, 1, 1)
        self.pastSyllablesLabel = QtGui.QLabel(Form)
        self.pastSyllablesLabel.setObjectName(_fromUtf8("pastSyllablesLabel"))
        self.gridLayout.addWidget(self.pastSyllablesLabel, 7, 0, 1, 1)
        self.pastSyl = QtGui.QSpinBox(Form)
        self.pastSyl.setMaximumSize(QtCore.QSize(60, 16777215))
        self.pastSyl.setObjectName(_fromUtf8("pastSyl"))
        self.gridLayout.addWidget(self.pastSyl, 7, 1, 1, 1)
        spacerItem2 = QtGui.QSpacerItem(20, 20, QtGui.QSizePolicy.Minimum, QtGui.QSizePolicy.Fixed)
        self.gridLayout.addItem(spacerItem2, 8, 1, 1, 1)
        self.pastParticipleLabel = QtGui.QLabel(Form)
        self.pastParticipleLabel.setObjectName(_fromUtf8("pastParticipleLabel"))
        self.gridLayout.addWidget(self.pastParticipleLabel, 9, 0, 1, 1)
        self.tPastpart = QtGui.QLineEdit(Form)
        self.tPastpart.setObjectName(_fromUtf8("tPastpart"))
        self.gridLayout.addWidget(self.tPastpart, 9, 1, 1, 1)
        self.pastPartSyllablesLabel = QtGui.QLabel(Form)
        self.pastPartSyllablesLabel.setObjectName(_fromUtf8("pastPartSyllablesLabel"))
        self.gridLayout.addWidget(self.pastPartSyllablesLabel, 10, 0, 1, 1)
        self.pastpartSyl = QtGui.QSpinBox(Form)
        self.pastpartSyl.setMaximumSize(QtCore.QSize(60, 16777215))
        self.pastpartSyl.setObjectName(_fromUtf8("pastpartSyl"))
        self.gridLayout.addWidget(self.pastpartSyl, 10, 1, 1, 1)
        spacerItem3 = QtGui.QSpacerItem(20, 20, QtGui.QSizePolicy.Minimum, QtGui.QSizePolicy.Fixed)
        self.gridLayout.addItem(spacerItem3, 11, 1, 1, 1)
        self.checkBox = QtGui.QCheckBox(Form)
        self.checkBox.setObjectName(_fromUtf8("checkBox"))
        self.gridLayout.addWidget(self.checkBox, 12, 1, 1, 1)
        self.checkBox_2 = QtGui.QCheckBox(Form)
        self.checkBox_2.setObjectName(_fromUtf8("checkBox_2"))
        self.gridLayout.addWidget(self.checkBox_2, 13, 1, 1, 1)
        self.horizontalLayout = QtGui.QHBoxLayout()
        self.horizontalLayout.setObjectName(_fromUtf8("horizontalLayout"))
        self.checkBox_4 = QtGui.QCheckBox(Form)
        self.checkBox_4.setObjectName(_fromUtf8("checkBox_4"))
        self.horizontalLayout.addWidget(self.checkBox_4)
        self.checkBox_3 = QtGui.QCheckBox(Form)
        self.checkBox_3.setObjectName(_fromUtf8("checkBox_3"))
        self.horizontalLayout.addWidget(self.checkBox_3)
        self.checkBox_5 = QtGui.QCheckBox(Form)
        self.checkBox_5.setObjectName(_fromUtf8("checkBox_5"))
        self.horizontalLayout.addWidget(self.checkBox_5)
        self.gridLayout.addLayout(self.horizontalLayout, 14, 1, 1, 1)
        spacerItem4 = QtGui.QSpacerItem(20, 2000, QtGui.QSizePolicy.Minimum, QtGui.QSizePolicy.Maximum)
        self.gridLayout.addItem(spacerItem4, 15, 1, 1, 1)
        self.horizontalLayout_3 = QtGui.QHBoxLayout()
        self.horizontalLayout_3.setObjectName(_fromUtf8("horizontalLayout_3"))
        spacerItem5 = QtGui.QSpacerItem(40, 20, QtGui.QSizePolicy.Expanding, QtGui.QSizePolicy.Minimum)
        self.horizontalLayout_3.addItem(spacerItem5)
        self.bAdd = QtGui.QPushButton(Form)
        self.bAdd.setObjectName(_fromUtf8("bAdd"))
        self.horizontalLayout_3.addWidget(self.bAdd)
        self.gridLayout.addLayout(self.horizontalLayout_3, 16, 1, 1, 1)
        self.horizontalLayout_6.addLayout(self.gridLayout)

        self.retranslateUi(Form)
        QtCore.QMetaObject.connectSlotsByName(Form)

    def retranslateUi(self, Form):
        Form.setWindowTitle(_translate("Form", "Verbs", None))
        self.label.setText(_translate("Form", "Verbs (Present):", None))
        self.bRemove.setText(_translate("Form", "Remove Entry", None))
        self.singularLabel.setText(_translate("Form", "Present:", None))
        self.singularSyllablesLabel.setText(_translate("Form", "Present Syllables:", None))
        self.pluralLabel.setText(_translate("Form", "Participle:", None))
        self.pluralSyllablesLabel.setText(_translate("Form", "Participle Syllables:", None))
        self.pastLabel.setText(_translate("Form", "Past:", None))
        self.pastSyllablesLabel.setText(_translate("Form", "Past Syllables", None))
        self.pastParticipleLabel.setText(_translate("Form", "Past Participle", None))
        self.pastPartSyllablesLabel.setText(_translate("Form", "Past Part Syllables:", None))
        self.checkBox.setText(_translate("Form", "Intransitive", None))
        self.checkBox_2.setText(_translate("Form", "Transitive", None))
        self.checkBox_4.setText(_translate("Form", "Linking", None))
        self.checkBox_3.setText(_translate("Form", "To Nouns", None))
        self.checkBox_5.setText(_translate("Form", "To Adjetives", None))
        self.bAdd.setText(_translate("Form", "Add/Update Entry", None))

