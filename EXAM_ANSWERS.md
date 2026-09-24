# LICEO RIDE - Semi-Final Laboratory Exam Answers
Name: Aldree C. Zambrano
Section: BSIT 2-1

Mr. Ernesto Razo Jr. · OOP · Semi-Final Laboratory Exam · Page 16

## Question 1
Why is the list in RideManager typed ArrayList<Ride> and not ArrayList<Jeepney>?
Your answer:
Using ArrayList<Ride> lets the manager hold all kinds of vehicles like Jeepneys, Tricycles, and Taxis in just one list. If we used ArrayList<Jeepney> instead, the list would only accept Jeepney objects and reject any other vehicle type.

## Question 2
In showStudentDiscounts(), why must you check instanceof before the cast?
Your answer:
We need to check with instanceof first to make sure the ride actually has a student discount before converting it. If we try to cast something without a discount like a Taxi, the program will crash with a ClassCastException.